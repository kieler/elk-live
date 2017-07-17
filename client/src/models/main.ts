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

import elkjs = require('elkjs')
import https = require('https')
import $ = require('jquery')
import ac = require('devbridge-autocomplete')
if (ac) { }

const githubOwner = 'uruuru'
const githubRepo = 'models'

// Create Sprotty viewer
const sprottyContainer = createContainer()
sprottyContainer.bind(TYPES.ModelSource).to(LocalModelSource).inSingletonScope()
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource)
const actionDispatcher = sprottyContainer.get<IActionDispatcher>(TYPES.IActionDispatcher)

// request contents of the models repository
let lastSelection = ''
getContentsRecursively('')
  .then(function (data) {
    let files = collectFiles(data)
    $('#autocomplete').autocomplete({
      lookup: files,
      onSelect: function (suggestion) {
        if (lastSelection == suggestion) {
          return
        }
        lastSelection = suggestion
        getFileContent(suggestion.data)
          .then(graph => layout(graph))
      }
    })
  })

function refreshLayout() {
  $('#sprotty').css('top', $('#navbar').height() + 'px')
}

$(window).resize(refreshLayout)
$(document).ready(refreshLayout)

function layout(inputGraph: any) {
  elkjs.layout({
    graph: inputGraph,
    callback: (err, graph) => {
      if (err === null) {
        let sGraph = new ElkGraphJsonToSprotty().transform(graph);
        modelSource.setModel(sGraph)
        actionDispatcher.dispatch(new FitToScreenAction([]))
      }
    }
  })
}

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
      response.setEncoding('utf8');
      let body = '';
      response.on('data', c => body += c)
      response.on('end', function () {
        try {
          resolve(JSON.parse(body));
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
        var buf = Buffer.from(response.content, 'base64')
        try {
          resolve(JSON.parse(buf.toString()))
        } catch (err) {
          reject(err)
        }
      })
    })
}

function collectDirs(d) {
  var td = { value: d.name, data: d.path }
  return [td].concat(...(d.dirs || []).map(sd => collectDirs(sd)))
}

function collectFiles(dir) {
  return (dir.files || [])
    .map(function (f) { return { value: f.path, data: f.path } })
    .concat(...(dir.dirs || []).map(sd => collectFiles(sd)))
}
