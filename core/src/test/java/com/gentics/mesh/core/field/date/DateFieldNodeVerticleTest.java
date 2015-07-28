package com.gentics.mesh.core.field.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;

public class DateFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
		dateFieldSchema.setName("dateField");
		dateFieldSchema.setLabel("Some label");
		schema.addField(dateFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("dateField", new DateFieldImpl().setDate("01.01.1971"));
		DateFieldImpl field = response.getField("dateField");
		assertEquals("01.01.1971", field.getDate());

		response = updateNode("dateField", new DateFieldImpl().setDate("02.01.1971"));
		field = response.getField("dateField");
		assertEquals("02.01.1971", field.getDate());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("dateField", new DateFieldImpl().setDate("01.01.1971"));
		DateField field = response.getField("dateField");
		assertEquals("01.01.1971", field.getDate());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		container.createDate("dateField").setDate("01.01.1971");

		NodeResponse response = readNode(node);
		DateField deserializedDateField = response.getField("dateField");
		assertNotNull(deserializedDateField);
		assertEquals("01.01.1971", deserializedDateField.getDate());
	}
}