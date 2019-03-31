import java.util.ArrayList;
import java.util.Arrays;
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
		this.utxoPool = new UTXOPool();
		ArrayList<UTXO> allUTXO = utxoPool.getAllUTXO();
		for (UTXO utxo : allUTXO) {
			Transaction.Output output = utxoPool.getTxOutput(utxo);
			this.utxoPool.addUTXO(new UTXO(Arrays.copyOf(utxo.getTxHash(), utxo.getTxHash().length), utxo.getIndex()), output);
		}
	}

	/**
	 * @return true if:
	 * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
	 * (2) the signatures on each input of {@code tx} are valid,
	 * (3) no UTXO is claimed multiple times by {@code tx},
	 * (4) all of {@code tx}s output values are non-negative, and
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	 * values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		ArrayList<Transaction.Input> inputs = tx.getInputs();

//		if (isNotExistUtxoInPool(inputs, outputs)) return false;
		if (isInvalidInputSignature(tx, outputs, inputs)) return false;

		if (claimedMultipleTimes(inputs)) return false;
		if (negativeOutputValues(outputs)) return false;
		if (outputValuesAreGreater(outputs, inputs)) return false;

		return true;
	}

	/*
	 * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
	 */
	private boolean isNotExistUtxoInPool(ArrayList<Transaction.Input> inputs, ArrayList<Transaction.Output> outputs) {
/*
		ArrayList<UTXO> allUTXO = utxoPool.getAllUTXO();
		List<Transaction.Output> poolOutputs = new LinkedList<Transaction.Output>();
		for (UTXO utxo : allUTXO) {
			Transaction.Output output = utxoPool.getTxOutput(utxo);
			poolOutputs.add(output);
		}

		for (Transaction.Output output : outputs) {
			if (!contains(poolOutputs, output)) return true;
		}
*/
/*
		for (Transaction.Input input : inputs) {
			if (input.outputIndex >= outputs.size()) continue;
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
			if (txOutput == null) return true;
			Transaction.Output output = outputs.get(input.outputIndex);
			if (txOutput.value != output.value || !txOutput.address.equals(output.address)) return true;

//			if (!utxoPool.contains(utxo)) return false;

//			Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
//			Transaction.Output output = outputs.get(input.outputIndex);
//			if(!txOutput.equals(output))return false;
		}
*/

		for (int i = 0; i < outputs.size(); i++) {
			Transaction.Input input = findInput(i, inputs);
			if (input == null) return true;
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if (!utxoPool.contains(utxo)) return true;
			Transaction.Output txOutput = utxoPool.getTxOutput(utxo);
			Transaction.Output output = outputs.get(i);
			if(!txOutput.address.equals(output.address) || txOutput.value != output.value) return true;
		}
		return false;

	}

	private Transaction.Input findInput(int index, ArrayList<Transaction.Input> inputs) {
		for (Transaction.Input input : inputs) {
			if (input.outputIndex == index) return input;
		}
		return null;
	}

	private boolean contains(List<Transaction.Output> poolOutputs, Transaction.Output output) {
		for (Transaction.Output poolOutput : poolOutputs) {
			if (output.address.equals(poolOutput.address) && output.value == poolOutput.value) return true;
		}
		return false;
	}

	/*
	 * (2) the signatures on each input of {@code tx} are valid,
	 */
	private boolean isInvalidInputSignature(Transaction tx, ArrayList<Transaction.Output> outputs, ArrayList<Transaction.Input> inputs) {
/*
		for (int i = 0; i < inputs.size(); i++) {
			Transaction.Input input = inputs.get(i);
			if (input.outputIndex >= outputs.size()) continue;
			Transaction.Output output = outputs.get(input.outputIndex);
			if (output == null) continue;
//			if (output.address == null || output.address.getEncoded() == null || output.address.getEncoded().length == 0) {
//				return true;
//			}
			boolean result = Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature);
			if (!result) return true;
		}
*/
		for (int i = 0; i < inputs.size(); i++) {
			Transaction.Input input = inputs.get(i);
			Transaction.Output txOutput = utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
			if(txOutput==null) continue;
//			Transaction.Output output = outputs.get(input.outputIndex);
			boolean result = Crypto.verifySignature(txOutput.address, tx.getRawDataToSign(i), input.signature);
			if (!result) return true;
		}
		return false;
	}

	/*
	 * (3) no UTXO is claimed multiple times by {@code tx},
	 */
	private boolean claimedMultipleTimes(ArrayList<Transaction.Input> inputs) {
		Set<UTXO> utxoSet = new HashSet<UTXO>();
		for (Transaction.Input input : inputs) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if (utxoSet.contains(utxo)) return true;
			utxoSet.add(utxo);
		}
		return false;
	}

	/*
	 * (4) all of {@code tx}s output values are non-negative, and
	 */
	private boolean negativeOutputValues(ArrayList<Transaction.Output> outputs) {
		for (Transaction.Output output : outputs) {
			if (output.value < 0) return true;
		}
		return false;
	}

	/*
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	 * values;
	 */
	private boolean outputValuesAreGreater(ArrayList<Transaction.Output> outputs, ArrayList<Transaction.Input> inputs) {
		double inputSum = 0d;
		for (Transaction.Input input : inputs) {
			Transaction.Output output = utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
			if (output == null) continue;
			inputSum += output.value;
		}
		double outputSum = 0d;
		for (Transaction.Output output : outputs) {
			outputSum += output.value;
		}
		return inputSum < outputSum;
	}


	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions, checking each
	 * transaction for correctness, returning a mutually valid array of accepted transactions, and
	 * updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
//		return possibleTxs;
		List<Transaction> transactions = new ArrayList<Transaction>();
		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				ArrayList<Transaction.Input> inputs = tx.getInputs();
				ArrayList<Transaction.Output> outputs = tx.getOutputs();
				for (Transaction.Input input : inputs) {
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					if(input.outputIndex >= outputs.size()) continue;
					Transaction.Output output = outputs.get(input.outputIndex);
					utxoPool.addUTXO(utxo, output);
				}
				transactions.add(tx);
			}
		}
		return transactions.toArray(new Transaction[0]);
	}

}
