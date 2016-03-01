package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;

import rx.Observable;

/**
 * Common graph model interface for schema field containers.
 * 
 * @param <R>
 *            Response model class of the container (e.g.: {@link Schema})
 * @param <V>
 *            Vertex implementation class
 * @param <RE>
 *            Response reference model class of the container (e.g.: {@link SchemaReference})
 */
public interface GraphFieldSchemaContainer<R extends FieldSchemaContainer, V extends GraphFieldSchemaContainer<R, V, RE>, RE extends NameUuidReference<RE>>
		extends MeshCoreVertex<R, V>, ReferenceableElement<RE> {

	/**
	 * Return the schema that is stored within the container.
	 * 
	 * @return
	 */
	R getSchema();

	/**
	 * Set the schema for the container.
	 * 
	 * @param schema
	 */
	void setSchema(R schema);

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Return the next version of this schema.
	 * 
	 * @return
	 */
	V getNextVersion();

	/**
	 * Return the version of the container.
	 * 
	 * @param version
	 * @return
	 */
	V findVersion(String version);

	/**
	 * Set the next version of the schema container.
	 * 
	 * @param container
	 */
	void setNextVersion(V container);

	/**
	 * Return the previous version of this schema.
	 * 
	 * @return
	 */
	V getPreviousVersion();

	/**
	 * Set the previous version of the container.
	 * 
	 * @param container
	 */
	void setPreviousVersion(V container);

	/**
	 * Return the change for the previous version of the schema. Normally the previous change was used to build the schema.
	 * 
	 * @return
	 */
	SchemaChange<?> getPreviousChange();

	/**
	 * Return the change for the next version.
	 * 
	 * @return Can be null if no further changes exist
	 */
	SchemaChange<?> getNextChange();

	/**
	 * Set the next change for the schema. The next change is the first change in the chain of changes that lead to the new schema version.
	 * 
	 * @param change
	 */
	void setNextChange(SchemaChange<?> change);

	/**
	 * Set the previous change for the schema. The previous change is the last change in the chain of changes that was used to create the schema container.
	 * 
	 * @param change
	 */
	void setPreviousChange(SchemaChange<?> change);

	/**
	 * Generate a schema change list by comparing the schema with the specified schema update model which is extracted from the action context.
	 * 
	 * @param ac
	 *            Action context that provides the schema update request
	 * @param comparator
	 *            Comparator to be used to compare the schemas
	 * @param restModel
	 *            Rest model of the container that should be compared
	 * @return
	 */
	Observable<SchemaChangesListModel> diff(InternalActionContext ac, AbstractFieldSchemaContainerComparator<?> comparator,
			FieldSchemaContainer restModel);

	/**
	 * Return the latest container version.
	 * 
	 * @return Latest version
	 */
	V getLatestVersion();

	/**
	 * Apply changes which will be extracted from the action context.
	 * 
	 * @param ac
	 *            Action context that provides the migration request data
	 * @return
	 */
	Observable<GenericMessageResponse> applyChanges(InternalActionContext ac);

	/**
	 * Apply the given list of changes to the schema container. This method will invoke the schema migration process.
	 * 
	 * @param ac
	 * @param listOfChanges
	 */
	Observable<GenericMessageResponse> applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges);

	/**
	 * Load the container root vertex of the container.
	 * 
	 * @return
	 */
	RootVertex<V> getRoot();

}