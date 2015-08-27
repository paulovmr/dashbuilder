/**
 * Copyright (C) 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dashbuilder.dataset;

import org.dashbuilder.dataset.def.DataSetPreprocessor;
import static org.dashbuilder.dataset.filter.FilterFactory.notEqualsTo;
import static org.dashbuilder.dataset.ExpenseReportsData.*;

public class CityFilterDataSetPreprocessor implements DataSetPreprocessor {
    
    private String filteredCity;

    public CityFilterDataSetPreprocessor(String filteredCity) {
        this.filteredCity = filteredCity;
    }
    
 
    @Override
    public void preprocess(DataSetLookup lookup) {
        lookup.getFirstFilterOp().addFilterColumn(notEqualsTo(COLUMN_CITY, filteredCity));
    }
}
