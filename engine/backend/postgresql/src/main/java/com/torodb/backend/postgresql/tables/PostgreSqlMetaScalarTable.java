/*
 * ToroDB
 * Copyright © 2014 8Kdata Technology (www.8kdata.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.torodb.backend.postgresql.tables;

import com.torodb.backend.converters.jooq.FieldTypeConverter;
import com.torodb.backend.postgresql.tables.records.PostgreSqlMetaScalarRecord;
import com.torodb.backend.tables.MetaScalarTable;
import com.torodb.core.transaction.metainf.FieldType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.util.postgres.PostgresDataType;

@SuppressFBWarnings(value = {"EQ_DOESNT_OVERRIDE_EQUALS", "HE_HASHCODE_NO_EQUALS"})
public class PostgreSqlMetaScalarTable
    extends MetaScalarTable<String[], PostgreSqlMetaScalarRecord> {

  private static final long serialVersionUID = -2338985946298600866L;
  /**
   * The singleton instance of <code>torodb.collections</code>
   */
  public static final PostgreSqlMetaScalarTable SCALAR = new PostgreSqlMetaScalarTable();

  @Override
  public Class<PostgreSqlMetaScalarRecord> getRecordType() {
    return PostgreSqlMetaScalarRecord.class;
  }

  /**
   * Create a <code>torodb.collections</code> table reference
   */
  public PostgreSqlMetaScalarTable() {
    this(TABLE_NAME, null);
  }

  /**
   * Create an aliased <code>torodb.collections</code> table reference
   */
  public PostgreSqlMetaScalarTable(String alias) {
    this(alias, PostgreSqlMetaScalarTable.SCALAR);
  }

  private PostgreSqlMetaScalarTable(String alias, Table<PostgreSqlMetaScalarRecord> aliased) {
    this(alias, aliased, null);
  }

  private PostgreSqlMetaScalarTable(String alias, Table<PostgreSqlMetaScalarRecord> aliased,
      Field<?>[] parameters) {
    super(alias, aliased, parameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PostgreSqlMetaScalarTable as(String alias) {
    return new PostgreSqlMetaScalarTable(alias, this);
  }

  /**
   * Rename this table
   */
  public PostgreSqlMetaScalarTable rename(String name) {
    return new PostgreSqlMetaScalarTable(name, null);
  }

  @Override
  protected TableField<PostgreSqlMetaScalarRecord, String> createDatabaseField() {
    return createField(TableFields.DATABASE.fieldName, SQLDataType.VARCHAR.nullable(false), this,
        "");
  }

  @Override
  protected TableField<PostgreSqlMetaScalarRecord, String> createCollectionField() {
    return createField(TableFields.COLLECTION.fieldName, SQLDataType.VARCHAR.nullable(false), this,
        "");
  }

  @Override
  protected TableField<PostgreSqlMetaScalarRecord, String[]> createTableRefField() {
    return createField(TableFields.TABLE_REF.fieldName, PostgresDataType.VARCHAR.getArrayDataType()
        .nullable(false), this, "");
  }

  @Override
  protected TableField<PostgreSqlMetaScalarRecord, FieldType> createTypeField() {
    return createField(TableFields.TYPE.fieldName, FieldTypeConverter.TYPE.nullable(false), this,
        "");
  }

  @Override
  protected TableField<PostgreSqlMetaScalarRecord, String> createIdentifierField() {
    return createField(TableFields.IDENTIFIER.fieldName, SQLDataType.VARCHAR.nullable(false), this,
        "");
  }
}
