package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;

public class BooleanFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName("booleanField");
		booleanFieldSchema.setLabel("Some label");
		schema.addField(booleanFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		container.createBoolean("booleanField").setBoolean(true);

		NodeResponse response = readNode(node);
		BooleanFieldImpl deserializedBooleanField = response.getField("booleanField");
		assertNotNull(deserializedBooleanField);
		assertTrue(deserializedBooleanField.getValue());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("booleanField", new BooleanFieldImpl().setValue(true));
		BooleanFieldImpl field = response.getField("booleanField");
		assertTrue(field.getValue());

		response = updateNode("booleanField", new BooleanFieldImpl().setValue(false));
		field = response.getField("booleanField");
		assertFalse(field.getValue());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("booleanField", new BooleanFieldImpl().setValue(true));
		BooleanFieldImpl field = response.getField("booleanField");
		assertTrue(field.getValue());
	}

}