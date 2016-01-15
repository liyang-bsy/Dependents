/**
 * Copyright 2007 Charlie Hubbard and Brandon Goodin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package flexjson.transformer;

import flexjson.*;

public class ObjectTransformer extends AbstractTransformer {

    public void transform(Object object) {
        JSONContext context = getContext();
        Path path = context.getPath();
        ChainedSet visits = context.getVisits();
        try {
            if (!visits.contains(object)) {
                context.setVisits(new ChainedSet(visits));
                context.getVisits().add(object);
                // traverse object
                BeanAnalyzer analyzer = BeanAnalyzer.analyze( resolveClass(object) );
                TypeContext typeContext = context.writeOpenObject();
                for( BeanProperty prop : analyzer.getProperties() ) {
                    String name = prop.getName();
                    path.enqueue(name);
                    if( context.isIncluded(prop) && prop.isReadable() ) {
                        Object value = prop.getValue( object );
                        if (!context.getVisits().contains(value)) {

                            Transformer transformer = context.getTransformer(prop, value);

                            if(!(transformer instanceof Inline) || !((Inline)transformer).isInline()) {
                                if (!typeContext.isFirst()) context.writeComma();
                                typeContext.increment();
                                context.writeName(prop.getJsonName());
                            }
                            typeContext.setPropertyName(prop.getJsonName());

                            transformer.transform(value);
                        }
                    }
                    path.pop();
                }
                context.writeCloseObject();
                context.setVisits((ChainedSet) context.getVisits().getParent());

            } else {
            	TypeContext parentTypeContext = getContext().peekTypeContext();
            	if(parentTypeContext != null) {
            		parentTypeContext.decrement();
            	}
            }
        } catch (JSONException e) {
            throw e;
        } catch (Exception e) {
            throw new JSONException(String.format("%s: Error while trying to serialize.", path), e);
        }
    }

    protected Class resolveClass( Object obj ) {
        return obj.getClass();
    }

}
