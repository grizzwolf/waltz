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

import _ from "lodash";
import {initialiseData, invokeFunction} from "../../../common";
import {preventDefault, stopPropagation} from "../../browser-utils"
import {buildHierarchies, doSearch, prepareSearchNodes} from "../../../common/hierarchy-utils";
import template from "./multi-select-tree-control.html";


const bindings = {
    items: "<",
    onClick: "<?",
    onCheck: "<",
    onUncheck: "<",
    checkedItemIds: "<",
    expandedItemIds: "<"
};


const initialState = {
    expandedItemIds: [],
    expandedNodes: [],
    checkedMap: {},
    hierarchy: [],
    onCheck: id => console.log("default handler in multi-select-treecontrol for node id check: ", id),
    onUncheck: id => console.log("default handler in multi-select-treecontrol for node id uncheck: ", id),
    onClick: node => console.log("default handler in multi-select-treecontrol for node click: ", node)
};


function haltEvent() {
    preventDefault(event);
    stopPropagation(event);
}


function mkCheckedMap(nodes = [], checked = []) {
    return _.reduce(nodes, (acc, n) => {
        acc[n.id] = _.includes(checked, n.id);
        return acc;
    }, {});
}


function expandSelectedNodes(nodes = [], expandedIds = []) {
    function recurse(nodes, ids) {
        const filteredNodes = _.filter(nodes, n => ids.includes(n.id));
        const newParentIds = _.chain(filteredNodes)
            .filter(n => n.parentId)
            .map("parentId")
            .value();

        if(newParentIds.length > 0) {
            return [filteredNodes, recurse(nodes, newParentIds)];
        } else {
            return [filteredNodes];
        }
    }

    return _.flattenDeep(recurse(nodes, expandedIds));
}


function controller() {
    const vm = initialiseData(this, initialState);

    vm.treeOptions = {
        nodeChildren: "children",
        dirSelectable: true,
        equality: (a, b) => a && b && a.id === b.id,
        multiSelection: false,
        isSelectable: (node) => {
            return !_.get(node, "disable", false);
        }
    };

    vm.hasAnyChild = (node) => {
        return node.children && node.children.length > 0;
    };

    vm.onNodeClick = (node) => {
        invokeFunction(vm.onClick, node.id);
    };

    vm.onToggleCheck = (node) => {
        if (_.includes(vm.checkedItemIds, node.id)) {
            vm.onNodeUncheck(node.id)
        } else {
            vm.onNodeCheck(node.id)
        }
    };

    vm.onNodeCheck = (id) => {
        invokeFunction(vm.onCheck, id);
        haltEvent();
    };


    vm.onNodeUncheck = (id) => {
        invokeFunction(vm.onUncheck, id);
        haltEvent();
    };

    vm.isNonConcreteAndSelected = (node) => {
        return !node.concrete && vm.checkedItemIds.includes(node.id);
    };

    vm.$onChanges = changes => {
        if(changes.items) {
            vm.hierarchy = buildHierarchies(vm.items, false);
            vm.searchNodes = prepareSearchNodes(vm.items);
            vm.expandedNodes = expandSelectedNodes(vm.items, vm.expandedItemIds);
        }

        if(changes.expandedItemIds) {
            vm.expandedNodes = expandSelectedNodes(vm.items, vm.expandedItemIds);
        }

        vm.checkedMap = mkCheckedMap(vm.items, vm.checkedItemIds);
    };

    vm.searchTermsChanged = (termStr = "") => {
        const matchingNodes = doSearch(termStr, vm.searchNodes);
        vm.hierarchy = buildHierarchies(matchingNodes, false);

        vm.expandedNodes = termStr.length === 0
            ? expandSelectedNodes(vm.items, vm.expandedItemIds)
            : matchingNodes;
    };

    vm.clearSearch = () => {
        vm.searchTermsChanged("");
        vm.searchTerms = "";
        vm.expandedNodes = expandSelectedNodes(vm.items, vm.expandedItemIds);
    };
}


controller.$inject = [];


const component = {
    bindings,
    template,
    controller,
    transclude: true
};


export default component;
