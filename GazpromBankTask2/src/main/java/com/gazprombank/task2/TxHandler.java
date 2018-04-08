package com.gazprombank.task2;

import java.util.ArrayList;
import java.util.List;

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
        UTXOPool unigueUTXOPool = new UTXOPool();

        double inSum = 0;
        double outSum = 0;
        for (int i=0; i<tx.numInputs(); i++){

            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            if (!utxoPool.contains(utxo)) return false;

            Transaction.Output out = utxoPool.getTxOutput(utxo);
            inSum+=utxoPool.getTxOutput(utxo).value;
            if (!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), in.signature)){
                return false;
            }
            if(unigueUTXOPool.contains(utxo)) return false;
            unigueUTXOPool.addUTXO(utxo, out);
        }
        for(Transaction.Output out: tx.getOutputs()){
            if (out.value <0) return false;
            outSum += out.value;
        }
        return !(outSum > inSum);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> goodTransactios = new ArrayList<>();
        for (Transaction tx: possibleTxs){
            if (isValidTx(tx)) {
                goodTransactios.add(tx);
                for (Transaction.Input in: tx.getInputs()){
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                int i = 0;
                for (Transaction.Output out: tx.getOutputs()){
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                    i++;
                }
            }
        }
        Transaction[] tOut = new Transaction[goodTransactios.size()];
        tOut = goodTransactios.toArray(tOut);
        return tOut;
    }
}
