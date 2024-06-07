package net.ankio.common.config

object Config {
    var assetManagement: Boolean = false //是否开启资产管理
    var multiCurrency: Boolean = false //是否开启多币种
    var reimbursement: Boolean = false //是否开启报销
    var lending: Boolean = false //是否开启债务功能
    var multiBooks: Boolean = false //是否开启多账本
    var fee: Boolean = false //是否开启手续费

    override fun toString(): String {
        return "AccountingConfig(assetManagement=$assetManagement, multiCurrency=$multiCurrency, reimbursement=$reimbursement, lending=$lending, multiBooks=$multiBooks, fee=$fee)"
    }
}