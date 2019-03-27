package ir.izo.crypto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
	public boolean isValidTx(Transaction tx) {
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		ArrayList<Transaction.Input> inputs = tx.getInputs();

		/*
		 * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
		 */
		for (Transaction.Input input : inputs) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if (!utxoPool.contains(utxo)) return false;
//			Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
//			Transaction.Output output = outputs.get(input.outputIndex);
//			if(!txOutput.equals(output))return false;
		}

		/*
		 * (2) the signatures on each input of {@code tx} are valid,
		 */
		for (Transaction.Input input : inputs) {
			if (input.outputIndex >= outputs.size()) return false;
			Transaction.Output output = outputs.get(input.outputIndex);
			boolean result = Crypto.verifySignature(output.address, tx.getRawDataToSign(input.outputIndex), input.signature);
			if (!result) return false;
		}

		/*
		 * (3) no UTXO is claimed multiple times by {@code tx},
		 */
		Set<UTXO> utxoSet = new HashSet<UTXO>();
		for (Transaction.Input input : inputs) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if (utxoSet.contains(utxo)) return false;
			utxoSet.add(utxo);
		}

		/*
		 * (4) all of {@code tx}s output values are non-negative, and
		 */
		for (Transaction.Output output : outputs) {
			if (output.value < 0) return false;
		}


		/*
		 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
		 * values;
		 */
		Set<Integer> indexes = new HashSet<Integer>();
		if (inputs.size() != outputs.size()) return false;
		for (Transaction.Input input : inputs) {
			if (input.outputIndex < 0 || input.outputIndex >= outputs.size()) return false;
			indexes.add(input.outputIndex);
		}
		if (indexes.size() != outputs.size()) return false;

		return true;
	}

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		List<Transaction> transactions = new ArrayList<Transaction>();
		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				ArrayList<Transaction.Input> inputs = tx.getInputs();
				ArrayList<Transaction.Output> outputs = tx.getOutputs();
				for (Transaction.Input input : inputs) {
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					Transaction.Output output = outputs.get(input.outputIndex);
					utxoPool.addUTXO(utxo, output);
				}
				transactions.add(tx);
			}
		}
		return transactions.toArray(new Transaction[0]);
	}

}
