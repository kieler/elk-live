/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { ElkExample } from './elkex';

const PATH_REGEX = /.*examples\/content\/(.*)\.elkt/

module.exports = function(source) {
  // Turn the resource path into a unique ID
  const resourcePath = <string>(<any>this).resourcePath;
  const match = PATH_REGEX.exec(resourcePath)
  // TODO handle error
  const path = match![1]
  const example = new ElkExample(path, source);
  const content = JSON.stringify(example);

  return `module.exports = ${content}`;
}