/*******************************************************************************
 * Copyright (c) 2017 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata'

import { TYPES, LocalModelSource, FitToScreenAction, IActionDispatcher } from 'sprotty/lib'
import { ElkGraphJsonToSprotty } from '../json/elkgraph-to-sprotty'
import createContainer from '../sprotty-config'
import { getParameters, combineParameters } from '../url-parameters'
import ELK from 'elkjs/lib/elk-api.js'

import https = require('https')
import $ = require('jquery')
import JSON5 = require('json5')
require('devbridge-autocomplete')

const urlParameters = getParameters()

let githubOwner = 'eclipse'
let githubRepo = 'elk-models'

// Create Sprotty viewer
const sprottyContainer = createContainer()
sprottyContainer.bind(TYPES.ModelSource).to(LocalModelSource).inSingletonScope()
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource)
const actionDispatcher = sprottyContainer.get<IActionDispatcher>(TYPES.IActionDispatcher)

// Set up ELK
const elk = new ELK({
  workerUrl: './elk/elk-worker.min.js'
})

// Div with loading indicator
const loading = <HTMLElement> document.getElementById('loading')
function setLoading(load: boolean) {
  if (load) {
    loading.style.display = 'block'
  } else {
    loading.style.display = 'none'
  }
}

// Div to show errors
const errorDiv = <HTMLElement> document.getElementById('error')
function showError(err: any) {
  if (err && err.message) {
    errorDiv.innerHTML = err.message
  } else {
    errorDiv.innerHTML = "A problem ocurred while loading the model."
  }
  errorDiv.style.display = 'block'
}

function updateSprottyModel(graph: any) {
  let sGraph = new ElkGraphJsonToSprotty().transform(graph);
  modelSource.setModel(sGraph)
  actionDispatcher.dispatch(new FitToScreenAction([]))
}

function loadModel(path: string) {
  setLoading(true)
  errorDiv.style.display = 'none'
  getFileContent(path)
    .then((g) => elk.layout(g))
    .then(updateSprottyModel)
    .then(() => {
      let encodedPath = encodeURIComponent(path)
      let queryString = combineParameters({link: encodedPath, owner: githubOwner, repo: githubRepo})
      window.history.pushState("", "", queryString)
    })
    .then(() => setLoading(false))
    .catch((err) => {
      setLoading(false)
      if (err) {
        console.error(err)
        showError(err)
      }
    })
}

// Initial model
let currentModel = ''
if (urlParameters.link) {
  currentModel = decodeURIComponent(urlParameters.link)
  // not yet supported
  //githubOwner = owner || githubOwner
  //githubRepo = repo || githubRepo
  $('#autocomplete').val(currentModel)
  loadModel(currentModel)
}

function initAutocomplete(files: any) {
  ($('#autocomplete') as any).autocomplete({
      lookup: files,
      minChars: 0,
      onSelect: function (suggestion) {
        let path = suggestion.value
        if (currentModel != path) {
          currentModel = path
          loadModel(currentModel)
        }
      }
    })
}

// Request contents of the models repository
getContentsRecursively('')
  .then((data) => {
    let files = collectFiles(data)
    initAutocomplete(files)
  })
  .catch((err) => showError(err))

function refreshLayout() {
  $('#sprotty').css('top', $('#navbar').height() + 'px')
}

$(window).resize(refreshLayout)
$(document).ready(setTimeout(refreshLayout, 50) as any)

function githubRequest(path) {
  return {
    host: 'api.github.com',
    path: '/repos/' + githubOwner + '/' + githubRepo + '/contents/' + path,
    headers: {
      'User-Agent': 'elk-models-viewer'
    }
  }
}

function asyncGet(req) {
  return new Promise(function (resolve, reject) {
    https.get(req, function (response) {
      if (response.statusCode !== 200) {
        reject(new Error(`Request failed with code: ${response.statusCode}`))
      }
      response.setEncoding('utf8');
      let body = '';
      response.on('data', c => body += c)
      response.on('end', function () {
        try {
          resolve(JSON5.parse(body));
        } catch (e) {
          reject(e)
        }
      })
      response.on('error', reject);
    }).on('error', reject);
  })
}

function getContentsRecursively(parentDir) {
  let path = parentDir.path || ''
  return asyncGet(githubRequest(path))
    .then(function (response: any) {
      if (!Array.isArray(response)) {
        return Promise.reject(new Error(`Unexpected response: ${response}.`))
      }
      var dir: any = {
        name: parentDir.name || '/',
        path: path,
        files: response.filter(e => e.type == 'file').map(f => {
          return {
            name: f.name,
            path: f.path
          }
        })
      }
      return Promise.all(response.filter(e => e.type == 'dir').map(d => {
        return getContentsRecursively(d)
          .then(function (subdir) {
            return subdir
          })
      })).then(function (subDirs) {
        dir.dirs = subDirs
        return dir
      })
    })
}

function getFileContent(filePath) {
  return asyncGet(githubRequest(filePath))
    .then(function (response: any) {
      return new Promise(function (resolve, reject) {
        try {
          var buf = Buffer.from(response.content, 'base64')
          resolve(JSON5.parse(buf.toString()))
        } catch (err) {
          reject(err)
        }
      })
    })
}

function collectFiles(dir) {
  return (dir.files || [])
    .filter(function (f) { return f.path.endsWith('.json') })
    .map(function (f) { return { value: f.path, data: f.path } })
    .concat(...(dir.dirs || []).map(sd => collectFiles(sd)))
}
