/*
 * Comet Keyboard Library
 * Copyright (C) 2011-2018 Comet Inc.
 * All Rights Reserved
 */
package com.comet.data.api

import com.comet.domain.entities.Dictionary

data class DictionaryItem(
    override var language: String,
    override var version: Int,
    override var filename: String,
    override var size: Int) : Dictionary {

    var isNeedUpdate = false
    var isInstalled = false
}
