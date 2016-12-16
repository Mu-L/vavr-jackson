/**
 * Copyright 2015 The Javaslang Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaslang.jackson.datatype.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

import javaslang.control.Either;

import static com.fasterxml.jackson.core.JsonToken.*;

class EitherDeserializer extends ValueDeserializer<Either<?, ?>> {

    private static final long serialVersionUID = 1L;

    private final JavaType javaType;
    private JsonDeserializer<?> stringDeserializer;

    EitherDeserializer(JavaType valueType) {
        super(valueType, 2);
        this.javaType = valueType;
    }

    @Override
    public Either<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final JsonToken nextToken = p.getCurrentToken();

        if (nextToken == START_ARRAY) {
            boolean right = false;
            Object value = null;
            int cnt = 0;

            for (JsonToken jsonToken = p.nextToken(); jsonToken != END_ARRAY; jsonToken = p.nextToken()) {
                cnt++;
                switch (cnt) {
                    case 1:
                        String def = (String) stringDeserializer.deserialize(p, ctxt);
                        if ("right".equals(def) || "r".equals(def)) {
                            right = true;
                        } else if ("left".equals(def) || "l".equals(def)) {
                            right = false;
                        } else {
                            throw ctxt.mappingException(javaType.getRawClass());
                        }
                        break;
                    case 2:
                        JsonDeserializer<?> deserializer = right ? deserializer(1) : deserializer(0);
                        value = (jsonToken != VALUE_NULL) ? deserializer.deserialize(p, ctxt) : deserializer.getNullValue(ctxt);
                        break;
                }
            }
            if (cnt != 2) {
                throw ctxt.mappingException(javaType.getRawClass());
            }
            return right ? Either.right(value) : Either.left(value);
        } else if (nextToken == START_OBJECT) {
            final String type = p.nextFieldName();
            if ("r".equals(type)) {
                final JsonDeserializer<?> deserializer = deserializer(1);
                final Object value = p.nextToken() != VALUE_NULL ? deserializer.deserialize(p, ctxt) : deserializer.getNullValue(ctxt);
                return Either.right(value);
            } else if ("l".equals(type)) {
                final JsonDeserializer<?> deserializer = deserializer(0);
                final Object value = p.nextToken() != VALUE_NULL ? deserializer.deserialize(p, ctxt) : deserializer.getNullValue(ctxt);
                return Either.left(value);
            } else {
                throw ctxt.mappingException(javaType.getRawClass());
            }
        } else {
            throw ctxt.mappingException(javaType.getRawClass());
        }
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        super.resolve(ctxt);
        stringDeserializer = ctxt.findContextualValueDeserializer(ctxt.constructType(String.class), null);
    }
}
