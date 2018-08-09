package com.github.neo.core.transaction

import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType

class TransferTransaction : TransactionNeo(TransactionType.TransferTransaction) {
    override val addressU160ForVerifying: Array<Address>?
        get() = null
}
