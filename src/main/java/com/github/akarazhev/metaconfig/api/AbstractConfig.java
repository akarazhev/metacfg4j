/* Copyright 2019 Andrey Karazhev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.github.akarazhev.metaconfig.api;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implements the interface of <code>Configurable<code/>
 * and provides additional functionality for getting properties.
 */
abstract class AbstractConfig implements Configurable {

    /**
     * Returns a property by paths.
     *
     * @param index  a current path.
     * @param paths  paths
     * @param source a current property stream.
     * @return a property.
     */
    Optional<Property> getProperty(final int index, final String[] paths, final Stream<Property> source) {
        if (index < paths.length) {
            final Optional<Property> current = source.
                    filter(property -> paths[index].equals(property.getName())).findFirst();
            if (current.isPresent()) {
                return index == paths.length - 1 ?
                        current : getProperty(index + 1, paths, current.get().getProperties());
            }
        }

        return Optional.empty();
    }
}
