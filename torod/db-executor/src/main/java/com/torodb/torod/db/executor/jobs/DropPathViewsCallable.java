
package com.torodb.torod.db.executor.jobs;

import com.torodb.torod.core.dbWrapper.DbConnection;
import com.torodb.torod.core.exceptions.ToroException;
import com.torodb.torod.core.exceptions.ToroRuntimeException;

/**
 *
 */
public class DropPathViewsCallable extends TransactionalJob<Integer> {

    private final Report report;
    private final String collection;

    public DropPathViewsCallable(
            DbConnection connection,
            TransactionAborter abortCallback,
            Report report,
            String collection) {
        super(connection, abortCallback);
        this.report = report;
        this.collection = collection;
    }

    @Override
    protected Integer failableCall() throws ToroException,
            ToroRuntimeException {

        Integer result = getConnection().dropPathViews(collection);
        report.dropViewsExecuted(collection, result);

        return result;
    }

    public static interface Report {
        public void dropViewsExecuted(
            String collection,
            Integer result);
    }
}
