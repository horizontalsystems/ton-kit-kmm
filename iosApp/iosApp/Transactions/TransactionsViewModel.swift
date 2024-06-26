import Combine
import Foundation
import TonKitKmm

class TransactionsViewModel: ObservableObject {
    private let tonKit = Singleton.instance.tonKit
    private var cancellables = Set<AnyCancellable>()

    @Published var transactions = [Transaction]()

    init() {
        collect(tonKit.doNewTransactionsPublisher)
            .completeOnFailure()
            .sink { [weak self] _ in
                self?.fetch()
            }
            .store(in: &cancellables)

        fetch()
    }

    private func fetch() {
        Task { [tonKit] in
            let tonTransactions = try await tonKit.transactions(fromTransactionHash: nil, type: nil, address: nil, limit: 100)

            DispatchQueue.main.async { [weak self] in
                self?.handle(tonTransactions: tonTransactions)
            }
        }
    }

    private func handle(tonTransactions: [TonTransactionWithTransfers]) {
        let transactions = tonTransactions.map { tx in
            Transaction(
                hash: tx.hash,
                lt: Int(tx.lt),
                timestamp: Int(tx.timestamp),
                fee: tx.fee.map { Singleton.amount(kitAmount: $0) },
                memo: tx.memo,
                type: tx.type.description(),
                transfers: tx.transfers.map { kitTransfer in
                    Transaction.Transfer(
                        src: kitTransfer.src.getNonBounceable(),
                        dest: kitTransfer.dest.getNonBounceable(),
                        amount: Singleton.amount(kitAmount: kitTransfer.amount)
                    )
                }
            )
        }

        self.transactions = transactions
    }
}

struct Transaction {
    let hash: String
    let lt: Int
    let timestamp: Int
    let fee: Decimal?
    let memo: String?
    let type: String
    let transfers: [Transfer]

    struct Transfer {
        let src: String
        let dest: String
        let amount: Decimal
    }
}
