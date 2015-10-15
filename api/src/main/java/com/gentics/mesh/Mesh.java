package com.gentics.mesh;

import java.util.Properties;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;

public interface Mesh {

	public static final String STARTUP_EVENT_ADDRESS = "mesh-startup-complete";

	/**
	 * Returns the initialized instance.
	 * 
	 * @return
	 * @throws MeshConfigurationException
	 */
	static Mesh mesh() {
		return factory.mesh();
	}

	/**
	 * Returns the initialized instance of mesh that was created using the given options.
	 * 
	 * @param options
	 * @return
	 */
	static Mesh mesh(MeshOptions options) {
		return factory.mesh(options);
	}

	/**
	 * Return the mesh version and build timestamp.
	 * 
	 * @return
	 */
	static String getVersion() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(Mesh.class.getResourceAsStream("/mesh.build.properties"));
			return buildProperties.get("mesh.version") + " " + buildProperties.get("mesh.build.timestamp");
		} catch (Exception e) {
			return "Unknown";
		}
		// Package pack = MeshImpl.class.getPackage();
		// return pack.getImplementationVersion();
	}

	/**
	 * Stop the the Mesh instance and release any resources held by it.
	 * 
	 * The instance cannot be used after it has been closed.
	 */
	void shutdown();

	/**
	 * Set a custom verticle loader that will be invoked once all major components have been initialized.
	 * 
	 * @param verticleLoader
	 */
	void setCustomLoader(MeshCustomLoader<Vertx> verticleLoader);

	/**
	 * Return the mesh options.
	 * 
	 * @return
	 */
	MeshOptions getOptions();

	/**
	 * Start mesh.
	 * 
	 * @throws Exception
	 */
	void run() throws Exception;

	/**
	 * Return the vertx instance for mesh.
	 * 
	 * @return
	 */
	Vertx getVertx();

	/**
	 * Returns the used vertx instance for mesh.
	 * 
	 * @return
	 */
	public static Vertx vertx() {
		return factory.mesh().getVertx();
	}

	static final MeshFactory factory = ServiceHelper.loadFactory(MeshFactory.class);

}