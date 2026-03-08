package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.ExternalContentPort

class ExternalContentAdapterStub : ExternalContentPort {

    override val lottoNumbers: String? = "1,2,3,4,5,6"

    override val bible: String? = null

    override val english: String? = null
}
