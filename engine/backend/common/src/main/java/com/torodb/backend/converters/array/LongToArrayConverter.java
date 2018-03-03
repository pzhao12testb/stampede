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

package com.torodb.backend.converters.array;

import com.torodb.kvdocument.values.KvLong;
import org.jooq.tools.json.JSONValue;

import javax.json.JsonNumber;

/**
 *
 */
public class LongToArrayConverter implements ArrayConverter<JsonNumber, KvLong> {

  private static final long serialVersionUID = 1L;

  @Override
  public String toJsonLiteral(KvLong value) {
    return JSONValue.toJSONString(value.getValue());
  }

  @Override
  public KvLong fromJsonValue(JsonNumber value) {
    return KvLong.of(value.longValue());
  }
}
