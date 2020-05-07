/*******************************************************************************
 * Copyright (c) 2017, 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata';

import { TYPES, LocalModelSource, FitToScreenAction, IActionDispatcher } from 'sprotty/lib';
import { ElkGraphJsonToSprotty } from '../json/elkgraph-to-sprotty';
import createContainer from '../sprotty-config';
import { getParameters, combineParameters } from '../url-parameters';
import ELK, { ElkNode } from 'elkjs-latest/lib/elk-api.js';

const availableModels = require('./models.json')

const urlParameters = getParameters();

// Create Sprotty viewer
const sprottyContainer = createContainer();
sprottyContainer.bind(TYPES.ModelSource).to(LocalModelSource).inSingletonScope();
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource);
const actionDispatcher = sprottyContainer.get<IActionDispatcher>(TYPES.IActionDispatcher);

// Set up ELK
const elk = new ELK({
    workerUrl: './elk-latest/elk-worker.min.js'
});

// Div with loading indicator
const loading = <HTMLElement>document.getElementById('loading');
function setLoading(load: boolean) {
    if (load) {
        loading.style.display = 'inline-block';
    } else {
        loading.style.display = 'none';
    }
}

// Div to show errors
const errorDiv = <HTMLElement>document.getElementById('error');
function showError(err: any) {
    if (err && err.message) {
        errorDiv.innerHTML = err.message;
    } else {
        errorDiv.innerHTML = "A problem ocurred while loading the model.";
    }
    errorDiv.style.display = 'inline-block';
}

function updateSprottyModel(graph: any) {
    let sGraph = new ElkGraphJsonToSprotty().transform(graph);
    modelSource.setModel(sGraph);
    actionDispatcher.dispatch(new FitToScreenAction([]));
}

const subPathLastSlashIndex = location.pathname.lastIndexOf('/')
const subPath = subPathLastSlashIndex > 0 
                    ? location.pathname.substr(0, subPathLastSlashIndex + 1)
                    : "";
function loadModel(path: string) {
    setLoading(true);
    errorDiv.style.display = 'none';

    const url = `${location.protocol}//${location.host}/${subPath}elk-models/${path}`
    fetch(url)
        .then(response => response.json())
        .then(g => elk.layout(<ElkNode>g))
        .then(updateSprottyModel)
        .then(() => {
            const encodedPath = encodeURIComponent(path);
            const queryString = combineParameters({ link: encodedPath });
            window.history.pushState("", "", queryString);
        })
        .then(() => setLoading(false))
        .catch((err) => {
            setLoading(false);
            if (err) {
                console.error(err);
                showError(err);
            }
        });
}

// Initial model
let currentModel = '';
if (urlParameters.link) {
    currentModel = decodeURIComponent(urlParameters.link);
    $('#autocomplete').val(currentModel);
    loadModel(currentModel);
}

function initAutocomplete(files: any) {
    ($('#autocomplete') as any).autocomplete({
        lookup: files,
        minChars: 0,
        onSelect: suggestion => {
            let path = suggestion.value;
            if (currentModel != path) {
                currentModel = path;
                loadModel(currentModel);
            }
        }
    });
}

// Populate autocomplete with the available models
initAutocomplete(availableModels.map(f => ({ value: f, data: f })));

function refreshLayout() {
    $('#sprotty').css('top', $('#navbar').height() + 'px');
}

$(window).resize(refreshLayout);
$(document).ready(setTimeout(refreshLayout, 50) as any);
