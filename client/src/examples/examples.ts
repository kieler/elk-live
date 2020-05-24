/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata';
import * as showdown from 'showdown';
import { createMonacoEditor, createSprottyViewer, openWebSocketElkGraph } from '../common/creators';
import { getParameters } from "../common/url-parameters";
import { createExampleCategoryTree, ElkExample, ExampleCategory } from './elkex';
import { ChangeLayoutVersionAction } from '../common/language-diagram-server';

require('../common/elkt-language');

// TODO when there are a lot of examples it's inefficient to 
// bundle all of them. Instead the requested example should be 
// loaded on-demand. However, building the navigation currently requires
// loading the example, which it's not yet implemented that way.
// const availableExamples = require('./examples.json');

enum HistoryMode {
    IGNORE, PUSH_STATE, REPLACE_STATE
}

// - - - - Set up sprotty, monaco, and the websocket - - - -
const [, diagramServer, actionDispatcher] = createSprottyViewer();
const editor = createMonacoEditor('example-graph');
editor.updateOptions({ scrollBeyondLastLine: false });
openWebSocketElkGraph(diagramServer);

// - - - - Showdown markdown parser - - - -
showdown.setFlavor('github');
// simpleLineBreaks: false requires at least two line breaks to start a new paragraph
const showdownConverter = new showdown.Converter({ simpleLineBreaks: false });

// - - - - The document elements we'll be working with - - - - 
const tocUl = <HTMLUListElement>document.getElementById('toc');
const descriptionDiv = <HTMLDivElement>document.getElementById('example-description');
const title = <HTMLElement>document.getElementById('title');
const titleSmall = <HTMLElement>document.getElementById('title-small');
const versionSelect = <HTMLSelectElement>document.getElementById('elk-version');
const loading = document.getElementById('loading-sprotty')!;

// - - - - Build the navigation from all available examples - - - -
const importAll = (r) => r.keys().map(r);
const allExamples = importAll((<any>require).context('./content/', true, /\.(elkt)$/));
const categoryTree = createExampleCategoryTree(allExamples);
createNavigationDepthFirstAlphabetically(categoryTree);

// - - - - Register layout version selection with server - - - -
versionSelect.onchange = () => {
    loading.style.display = 'block';
    const selectedVersion = versionSelect.options[versionSelect.selectedIndex].value;
    actionDispatcher.dispatch(new ChangeLayoutVersionAction(selectedVersion));
}
// TODO see comment in elkgraph/editor.ts
editor.onDidChangeModelContent(() => loading.style.display = 'block');

// - - - - Load initial example. - - - -
const initialExamplePath = ((params) => {
    // Either the example id passed via the query parameter 'e' or a random example
    return params.e || allExamples[Math.floor(Math.random() * allExamples.length)].path;
})(getParameters());
loadExample(decodeURIComponent(initialExamplePath), HistoryMode.REPLACE_STATE);

window.onpopstate = (event) => {
    if (event.state !== undefined && event.state.e !== undefined) {
        loadExample(event.state.e);
    }
    return event.preventDefault();
}

/* - - - - Utility functions - - - - */

function loadExample(examplePath: string, historyMode: HistoryMode = HistoryMode.IGNORE) {
    try {
        const example = require(`./content/${examplePath}.elkt`);
        title.innerText = example.label;
        titleSmall.innerText = example.category.join(' > ');
        descriptionDiv.innerHTML = showdownConverter.makeHtml(example.doc);
        editor.setValue(example.graph);
        editor.setPosition({ lineNumber: 1, column: 1 });
        switch (historyMode) {
            case HistoryMode.PUSH_STATE:
                window.history.pushState({ e: example.path }, '', `${location.pathname}?e=${encodeURIComponent(example.path)}`)
                break;
            case HistoryMode.REPLACE_STATE:
                window.history.replaceState({ e: example.path }, '');
                break;
        }
    } catch(e) {
        // TODO it would be nice to have a speaking error.
        editor.setValue(e.message);
    }
}

function createNavigationHeading(name: string, indent: number) {
    const heading = document.createElement("h6");
    heading.className = `sidebar-heading text-muted pl-${indent} mt-2 mb-1`;
    const span = document.createElement("span");
    span.innerText = name;
    heading.appendChild(span);
    return heading;
}

function createNavigationLink(example: ElkExample, indent: number = 0) {
    const listElement = document.createElement("li");
    const link = document.createElement("button");
    link.type = 'button';
    link.className = `sidebar-link btn btn-link btn text-left s-m pl-${indent} py-0`;
    link.innerText = example.label;
    link.onclick = () => loadExample(example.path, HistoryMode.PUSH_STATE);
    listElement.appendChild(link);
    return listElement;
}

function createNavigationDepthFirstAlphabetically(category: ExampleCategory, indent: number = 0, namePrefix = "") {
    const name = category.name;
    if (name != 'root') {
        tocUl.appendChild(createNavigationHeading(namePrefix + name, indent));
    }
    if (category.elements !== undefined) {
        category.elements.sort((a, b) => a.label.localeCompare(b.label))
            .forEach(e => tocUl.appendChild(createNavigationLink(e, indent)));
    }
    const newPrefix = name != 'root' ? `${namePrefix} ${name} > ` : namePrefix;
    category.subCategories.sort((a, b) => a.name.localeCompare(b.name))
        .forEach(c => createNavigationDepthFirstAlphabetically(c, indent + 1, newPrefix));
}

