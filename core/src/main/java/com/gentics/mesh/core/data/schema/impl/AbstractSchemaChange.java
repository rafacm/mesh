package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.util.Tuple;

/**
 * @see SchemaChange
 */
public abstract class AbstractSchemaChange<T extends FieldSchemaContainer> extends MeshVertexImpl implements SchemaChange<T> {

	private static String MIGRATION_SCRIPT_PROPERTY_KEY = "migrationScript";

	public static final String REST_PROPERTY_PREFIX_KEY = "fieldProperty_";

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange) out(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setNextChange(SchemaChange<?> change) {
		setUniqueLinkOutTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange) in(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousChange(SchemaChange<?> change) {
		setUniqueLinkInTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	abstract public SchemaChangeOperation getOperation();

	@Override
	public <R extends GraphFieldSchemaContainer<?, ?, ?>> R getPreviousContainer() {
		return (R) in(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousContainer(GraphFieldSchemaContainer<?, ?, ?> container) {
		setSingleLinkInTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public <R extends GraphFieldSchemaContainer<?, ?, ?>> R getNextContainer() {
		return (R) out(HAS_SCHEMA_CONTAINER).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setNextSchemaContainer(GraphFieldSchemaContainer<?, ?, ?> container) {
		setSingleLinkOutTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public String getMigrationScript() throws IOException {
		String migrationScript = getProperty(MIGRATION_SCRIPT_PROPERTY_KEY);
		if (migrationScript == null) {
			migrationScript = getAutoMigrationScript();
		}

		return migrationScript;
	}

	@Override
	public SchemaChange<T> setCustomMigrationScript(String migrationScript) {
		setProperty(MIGRATION_SCRIPT_PROPERTY_KEY, migrationScript);
		return this;
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		return null; // Default value for changes that don't have a script
	}

	@Override
	public List<Tuple<String, Object>> getMigrationScriptContext() {
		return null; // Default value for changes that don't have a script
	}

	@Override
	public void setRestProperty(String key, Object value) {
		setProperty(REST_PROPERTY_PREFIX_KEY + key, value);
	}

	@Override
	public <T> T getRestProperty(String key) {
		return getProperty(REST_PROPERTY_PREFIX_KEY + key);
	}

	@Override
	public <T> Map<String, T> getRestProperties() {
		return getProperties(REST_PROPERTY_PREFIX_KEY);
	}

	@Override
	public void updateFromRest(SchemaChangeModel restChange) {
		String migrationScript = restChange.getMigrationScript();
		if (migrationScript != null) {
			setCustomMigrationScript(migrationScript);
		}
		for (String key : restChange.getProperties().keySet()) {
			setRestProperty(key, restChange.getProperties().get(key));
		}
	}

	@Override
	public SchemaChangeModel transformToRest() throws IOException {
		SchemaChangeModel model = new SchemaChangeModel();
		// Strip away the prefix
		for (String key : getRestProperties().keySet()) {
			Object value = getRestProperties().get(key);
			key = key.replace(REST_PROPERTY_PREFIX_KEY, "");
			model.getProperties().put(key, value);
		}
		model.setOperation(getOperation());
		model.setUuid(getUuid());
		model.setMigrationScript(getMigrationScript());
		return model;
	}

}