package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see UpdateMicroschemaChange
 */
public class UpdateMicroschemaChangeImpl extends AbstractFieldSchemaContainerUpdateChange<Microschema> implements UpdateMicroschemaChange {

	public static void checkIndices(Database database) {
		database.addVertexType(UpdateMicroschemaChangeImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

}