package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.core.rest.node.NodeRequestParameters.EXPANDFIELDS_QUERY_PARAM_KEY;
import static com.gentics.mesh.core.rest.node.NodeRequestParameters.LANGUAGES_QUERY_PARAM_KEY;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NamedNode;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class VerticleHelper {

	private static final Logger log = LoggerFactory.getLogger(VerticleHelper.class);

	public static final String QUERY_MAP_DATA_KEY = "queryMap";

	public static String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	/**
	 * Shortcut method to access projectRoot aggregation vertex.
	 * 
	 * @param rc
	 * @return
	 */
	public static Project getProject(RoutingContext rc) {
		return BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findByName(getProjectName(rc));
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel> void loadTransformAndResponde(RoutingContext rc, RootVertex<T> root,
			AbstractListResponse<TR> listResponse) {
		loadObjects(rc, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		} , listResponse);
	}

	public static void setPaging(AbstractListResponse<?> response, Page<?> page) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(page.getNumber());
		info.setPageCount(page.getTotalPages());
		info.setPerPage(page.getPerPage());
		info.setTotalCount(page.getTotalElements());
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void loadObjects(RoutingContext rc,
			RootVertex<T> root, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		PagingInfo pagingInfo = getPagingInfo(rc);
		MeshAuthUser requestUser = getUser(rc);
		try {

			Page<? extends T> page = root.findAll(requestUser, pagingInfo);
			for (T node : page) {
				node.transformToRest(rc, rh -> {
					if (hasSucceeded(rc, rh)) {
						listResponse.getData().add(rh.result());
					}
					// TODO handle async issue
				});
			}
			setPaging(listResponse, page);
			handler.handle(Future.succeededFuture(listResponse));
		} catch (InvalidArgumentException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadTransformAndResponde(RoutingContext rc, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root) {
		loadAndTransform(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		});
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformAndResponde(
			RoutingContext rc, Page<T> page, RL listResponse) {
		transformPage(rc, page, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		} , listResponse);
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformPage(RoutingContext rc,
			Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		for (T node : page) {
			node.transformToRest(rc, rh -> {
				listResponse.getData().add(rh.result());
			});
		}
		setPaging(listResponse, page);
		handler.handle(Future.succeededFuture(listResponse));
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadAndTransform(RoutingContext rc, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, Handler<AsyncResult<RestModel>> handler) {
		loadObject(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				// TODO handle nested exceptions differently
				try {
					rh.result().transformToRest(rc, th -> {
						if (hasSucceeded(rc, th)) {
							handler.handle(Future.succeededFuture(th.result()));
						} else {
							handler.handle(Future.failedFuture("Not authorized"));
						}
					});
				} catch (HttpStatusCodeErrorException e) {
					handler.handle(Future.failedFuture(e));
				}
			}
		});
	}

	public static MeshAuthUser getUser(RoutingContext routingContext) {
		if (routingContext.user() instanceof MeshAuthUser) {
			MeshAuthUser user = (MeshAuthUser) routingContext.user();
			return user;
		}
		// TODO i18n
		throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Could not load request user");
	}

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @param rc
	 * @return List of languages. List can be empty.
	 */
	public static List<String> getSelectedLanguageTags(RoutingContext rc) {
		List<String> languageTags = new ArrayList<>();
		Map<String, String> queryPairs = splitQuery(rc);
		if (queryPairs == null) {
			return new ArrayList<>();
		}
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value != null) {
			languageTags = new ArrayList<>(Arrays.asList(value.split(",")));
		}
		languageTags.add(Mesh.mesh().getOptions().getDefaultLanguage());
		return languageTags;

	}

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @param rc
	 * @return List of languages. List can be empty.
	 */
	public static List<String> getExpandedFieldnames(RoutingContext rc) {
		List<String> expandFieldnames = new ArrayList<>();
		Map<String, String> queryPairs = splitQuery(rc);
		if (queryPairs == null) {
			return new ArrayList<>();
		}
		String value = queryPairs.get(EXPANDFIELDS_QUERY_PARAM_KEY);
		if (value != null) {
			expandFieldnames = new ArrayList<>(Arrays.asList(value.split(",")));
		}
		return expandFieldnames;
	}

	public static Map<String, String> splitQuery(RoutingContext rc) {
		rc.data().computeIfAbsent(QUERY_MAP_DATA_KEY, map -> {
			String query = rc.request().query();
			Map<String, String> queryPairs = new LinkedHashMap<String, String>();
			if (query == null) {
				return queryPairs;
			}
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");

				try {
					queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Could not decode query string pair {" + pair + "}", e);
				}

			}
			return queryPairs;
		});
		return (Map<String, String>) rc.data().get(QUERY_MAP_DATA_KEY);
	}

	/**
	 * Extract the paging information from the request parameters. The paging information contains information about the number of the page that is currently
	 * requested and the amount of items that should be included in a single page.
	 * 
	 * @param rc
	 * @return Paging information
	 */
	public static PagingInfo getPagingInfo(RoutingContext rc) {
		MultiMap params = rc.request().params();
		int page = 1;
		int perPage = MeshOptions.DEFAULT_PAGE_SIZE;
		if (params != null) {
			page = NumberUtils.toInt(params.get("page"), 1);
			perPage = NumberUtils.toInt(params.get("perPage"), MeshOptions.DEFAULT_PAGE_SIZE);
		}
		if (page < 1) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, getI18n().get(rc, "error_invalid_paging_parameters"));
		}
		if (perPage <= 0) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, getI18n().get(rc, "error_invalid_paging_parameters"));
		}
		return new PagingInfo(page, perPage);
	}

	public static <T extends RestModel> void transformAndResponde(RoutingContext rc, GenericVertex<T> node) {
		node.transformToRest(rc, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		});
	}

	public static void responde(RoutingContext rc, String body) {
		rc.response().putHeader("content-type", AbstractWebVerticle.APPLICATION_JSON);
		// TODO use 201 for created entities
		rc.response().setStatusCode(200).end(body);
	}

	/**
	 * Calls the rc.fail method and sets a http 400 error with the additional response information.
	 * 
	 * @param rc
	 * @param msg
	 * @param parameters
	 */
	public static void fail(RoutingContext rc, String msg, String... parameters) {
		I18NService i18n = I18NService.getI18n();
		rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, msg, parameters)));
	}

	public static <T extends GenericVertex<?>> void createObject(RoutingContext rc, RootVertex<T> root) {
		root.create(rc, rh -> {
			if (hasSucceeded(rc, rh)) {
				GenericVertex<?> vertex = rh.result();
				transformAndResponde(rc, vertex);
				triggerEvent(vertex.getUuid(), vertex.getType(), SearchQueueEntryAction.CREATE_ACTION);
			}
		});
	}
	
//	public static <T extends GenericVertex<?>> void createObject(RoutingContext rc, RootVertex<T> root) {
//		final int RETRY_COUNT = 15;
////		Mesh.vertx().executeBlocking(bc -> {
//			AtomicBoolean hasFinished = new AtomicBoolean(false);
//			for (int i = 0; i < RETRY_COUNT && !hasFinished.get(); i++) {
//				try {
//					log.debug("Opening new transaction for try: {" + i + "}");
//					try (Trx tx = new Trx(MeshSpringConfiguration.getMeshSpringConfiguration().database())) {
//						if (log.isDebugEnabled()) {
//							log.debug("Invoking create on root vertex");
//						}
//						root.create(rc, rh -> {
//							if (rh.failed()) {
//								log.debug("Request for creation failed.", rh.cause());
//							} else {
//								GenericVertex<?> vertex = rh.result();
//								//triggerEvent(vertex.getUuid(), vertex.getType(), SearchQueueEntryAction.CREATE_ACTION);
//								try (Trx txRead = new Trx(MeshSpringConfiguration.getMeshSpringConfiguration().database())) {
//									vertex.reload();
//									transformAndResponde(rc, vertex);
//								}
//							}
//							hasFinished.set(true);
//						});
//					}
//				} catch (OConcurrentModificationException e) {
//					log.error("Creation failed in try {" + i + "} retrying.");
//				}
//			}
//			if (!hasFinished.get()) {
//				log.error("Creation failed after {" + RETRY_COUNT + "} attempts.");
//				rc.fail(new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Creation failed after {" + RETRY_COUNT + "} attepmts."));
//			}
////		} , false, rh -> {
////			if (rh.failed()) {
////				rc.fail(rh.cause());
////			}
////		});
//
//	}

	public static <T extends GenericVertex<?>> void updateObject(RoutingContext rc, String uuidParameterName, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		loadObject(rc, uuidParameterName, UPDATE_PERM, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				GenericVertex<?> vertex = rh.result();
				String uuid = vertex.getUuid();
				String type = vertex.getType();
				vertex.update(rc);
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				try (Trx tx = new Trx(db)) {
					transformAndResponde(rc, vertex);
				}
				triggerEvent(uuid, type, SearchQueueEntryAction.UPDATE_ACTION);
			}
		});
	}

	/**
	 * Trigger a search event for the given type and uuid and action.
	 * 
	 * @param uuid
	 * @param type
	 * @param action
	 */
	public static void triggerEvent(String uuid, String type, SearchQueueEntryAction action) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		Mesh.vertx().executeBlocking(bc -> {
			try (Trx tx = new Trx(db)) {
				BootstrapInitializer.getBoot().meshRoot().getSearchQueue().put(uuid, type, action);
				tx.success();
			}
			Mesh.vertx().eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
		} , false, rh -> {
			if (rh.failed()) {
				//TODO this should be handled and the request should fail. How can we rollback the update/create/delete? Should we retry?
				rh.cause().printStackTrace();
			}
		});
	}

	public static <T extends GenericVertex<? extends RestModel>> void deleteObject(RoutingContext rc, String uuidParameterName, String i18nMessageKey,
			RootVertex<T> root) {
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		loadObject(rc, uuidParameterName, DELETE_PERM, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				GenericVertex<?> vertex = rh.result();
				String uuid = vertex.getUuid();
				String name = null;
				String type = vertex.getType();
				if (vertex instanceof NamedNode) {
					name = ((NamedNode) vertex).getName();
				}
				try (Trx tx = new Trx(db)) {
					vertex.delete();
					tx.success();
				}
				String id = name != null ? uuid + "/" + name : uuid;
				responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, i18nMessageKey, id))));
				triggerEvent(uuid, type, SearchQueueEntryAction.DELETE_ACTION);
			}
		});
	}

	public static <T extends GenericVertex<?>> void loadObject(RoutingContext rc, String uuidParameterName, GraphPermission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {

		I18NService i18n = I18NService.getI18n();
		String uuid = rc.request().params().get(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(Future
					.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_request_parameter_missing", uuidParameterName))));
		} else {
			loadObjectByUuid(rc, uuid, perm, root, handler);
		}
	}

	public static <T extends GenericVertex<?>> void loadObjectByUuid(RoutingContext rc, String uuid, GraphPermission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {
		if (root == null) {
			// TODO i18n
			handler.handle(Future.failedFuture("Could not find root node."));
		} else {
			I18NService i18n = I18NService.getI18n();
			root.findByUuid(uuid, rh -> {
				try (Trx tx = new Trx(MeshSpringConfiguration.getMeshSpringConfiguration().database())) {
					if (rh.failed()) {
						handler.handle(Future.failedFuture(rh.cause()));
					} else {
						T node = rh.result();
						if (node == null) {
							handler.handle(Future.failedFuture(new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid))));
						} else {
							MeshAuthUser requestUser = getUser(rc);
							if (requestUser.hasPermission(node, perm)) {
								handler.handle(Future.succeededFuture(node));
							} else {
								handler.handle(
										Future.failedFuture(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", node.getUuid()))));
							}
						}
					}
				}
			});
		}
	}

	public static boolean hasSucceeded(RoutingContext rc, AsyncResult<?> result) {
		if (result.failed()) {
			rc.fail(result.cause());
			return false;
		}
		return true;
	}

}
