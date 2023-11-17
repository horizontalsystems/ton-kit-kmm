package io.horizontalsystems.tonkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TonKit(
    private val transactionManager: TransactionManager,
    private val balanceManager: BalanceManager,
    val receiveAddress: String,
    private val syncer: Syncer,
    private val transactionSender: TransactionSender?
) {
    val newTransactionsFlow by transactionManager::newTransactionsFlow
    val balanceFlow by balanceManager::balanceFlow

    val balanceSyncStateFlow by syncer::balanceSyncStateFlow
    val transactionsSyncStateFlow by syncer::transactionsSyncStateFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        syncer.start()
    }

    fun stop() {
        coroutineScope.cancel()
        syncer.stop()
    }

    suspend fun send(dest: String, amount: String) {
        checkNotNull(transactionSender) {
            "Sending is not available for watch account"
        }

        transactionSender.send(dest, amount)
    }

    suspend fun transactions(fromTransactionHash: String?, type: TransactionType?, limit: Long): List<TonTransaction> {
        return transactionManager.transactions(fromTransactionHash, type, limit)
    }

    fun onNewTransactions(callback: (List<TonTransaction>) -> Unit) {
        coroutineScope.launch {
            newTransactionsFlow.collect(callback)
        }
    }

    fun onBalance(callback: (String?) -> Unit) {
        coroutineScope.launch {
            balanceFlow.collect(callback)
        }
    }

    fun onBalanceSyncState(callback: (SyncState) -> Unit) {
        coroutineScope.launch {
            balanceSyncStateFlow.collect(callback)
        }
    }

    fun onTransactionsSyncState(callback: (SyncState) -> Unit) {
        coroutineScope.launch {
            transactionsSyncStateFlow.collect(callback)
        }
    }
}
