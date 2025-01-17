package org.zenframework.z8.server.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.base.table.value.Field;
import org.zenframework.z8.server.engine.IDatabase;
import org.zenframework.z8.server.logs.Trace;

public class Insert extends Statement {
	private Query query;
	private Collection<Field> fields;

	public Insert(Query query, Collection<Field> fields) {
		super(query.getConnection());

		this.query = query;

		this.fields = new ArrayList<Field>();

		for(Field field : fields) {
			if(!field.isExpression())
				this.fields.add(field);
		}

		sql = buildSql();
	}

	private String buildSql() {
		IDatabase database = database();
		DatabaseVendor vendor = vendor();

		String insertFields = "";
		String insertValues = "";

		for(Field field : fields) {
			insertFields += (insertFields.isEmpty() ? "" : ", ") + vendor.quote(field.name());
			insertValues += (insertValues.isEmpty() ? "" : ", ") + "?";
		}

		return "insert into " + database.tableName(query.name()) + " " + "(" + insertFields + ") values (" + insertValues + ")";
	}

	public void execute() {
		try {
			prepare(sql, query.priority());
			executeUpdate();
		} catch(Throwable e) {
			Trace.logEvent(sql());

			for(Field field : fields)
				Trace.logEvent(field.name() + ": " + field.getDefault());

			throw new RuntimeException(e);
		} finally {
			close();
		}
	}

	@Override
	public void prepare(String sql, int priority) throws SQLException {
		super.prepare(sql, priority);

		int position = 1;

		for(Field field : fields) {
			set(position, field,  field.getDefault());
			position++;
		}
	}
}
