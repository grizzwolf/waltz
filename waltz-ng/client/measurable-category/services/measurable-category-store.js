/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */


function store($http, baseApiUrl) {
    const baseUrl = `${baseApiUrl}/measurable-category`;

    const findAll = () => $http
        .get(`${baseUrl}/all`)
        .then(d => d.data);


    const getById = (id) => $http
        .get(`${baseUrl}/id/${id}`)
        .then(x => x.data);

    return {
        findAll,
        getById
    };

}

store.$inject = ["$http", "BaseApiUrl"];


const serviceName = "MeasurableCategoryStore";

export default {
    serviceName,
    store
};

export const MeasurableCategoryStore_API = {
    findAll: {
        serviceName,
        serviceFnName: "findAll",
        description: "finds all categories"
    },
    getById: {
        serviceName,
        serviceFnName: "getById",
        description: "retrieves a single category"
    }
};