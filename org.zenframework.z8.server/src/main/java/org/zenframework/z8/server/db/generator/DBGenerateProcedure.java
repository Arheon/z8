package org.zenframework.z8.server.db.generator;

import java.util.Collection;

import org.zenframework.z8.server.base.form.Desktop;
import org.zenframework.z8.server.base.job.scheduler.Scheduler;
import org.zenframework.z8.server.base.simple.Procedure;
import org.zenframework.z8.server.base.table.Table;
import org.zenframework.z8.server.db.Connection;
import org.zenframework.z8.server.db.ConnectionManager;
import org.zenframework.z8.server.engine.ApplicationServer;
import org.zenframework.z8.server.engine.Runtime;
import org.zenframework.z8.server.logs.Trace;
import org.zenframework.z8.server.resources.Resources;
import org.zenframework.z8.server.runtime.IObject;
import org.zenframework.z8.server.security.SecurityGroup;
import org.zenframework.z8.server.utils.ErrorUtils;

public class DBGenerateProcedure extends Procedure {
	public static class CLASS<T extends DBGenerateProcedure> extends Procedure.CLASS<T> {
		public CLASS(IObject container) {
			super(container);
			setJavaClass(DBGenerateProcedure.class);
			setDisplayName(Resources.get("Generator.displayName"));
		}

		@Override
		public Object newObject(IObject container) {
			return new DBGenerateProcedure(container);
		}
	}

	public DBGenerateProcedure(IObject container) {
		super(container);
		useTransaction.set(false);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void z8_execute() {
		super.z8_execute();

		if(ApplicationServer.getUser().securityGroup() != SecurityGroup.Administrators) {
			print("You must be a member of Administrators security group to perform this action.");
			reportProgress(100);
			return;
		}

		reportProgress(0);
		Scheduler.stop();
		reportProgress(Resources.get("Generator.schedulerStopped"), 0);

		Logger logger = new Logger();

		try {
			Connection connection = ConnectionManager.get();

			Collection<Table.CLASS<? extends Table>> tables = Runtime.instance().tables();
			Collection<Desktop.CLASS<? extends Desktop>> entries = (Collection)Runtime.instance().entries();

			try {
				DBGenerator generator = new DBGenerator(connection);
				generator.run(tables, entries, logger);
			} catch(Throwable e) {
				logger.error(e);
			}
		} catch(Throwable e) {
			logger.error(e);
		}

		Scheduler.start();

		reportProgress(Resources.get("Generator.schedulerStarted"), 100);
	}

	private class Logger implements ILogger {
		@Override
		public void error(Throwable exception, String message) {
			Trace.logError(message, exception);
			print(message);
		}

		@Override
		public void error(Throwable exception) {
			error(exception, ErrorUtils.getMessage(exception));
		}

		@Override
		public void message(String message) {
			print(message);
		}

		@Override
		public void progress(int percentDone) {
			reportProgress(percentDone);
		}
	}

}
