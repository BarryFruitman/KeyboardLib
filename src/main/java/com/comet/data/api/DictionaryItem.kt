/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.data.api

data class DictionaryItem(
	var language: String,
    var version: Int,
    var filename: String,
    var size: Int) {

    var isNeedUpdate = false
    var isInstalled = false
}
