package com.klaytn.caver.transaction;

import com.klaytn.caver.crypto.KlaySignatureData;
import com.klaytn.caver.transaction.type.TransactionType;
import com.klaytn.caver.utils.Utils;
import org.web3j.rlp.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class LegacyTransaction extends AbstractTransaction {
    /**
     * The account address that will receive the transferred value.
     */
    String to;

    /**
     * Data attached to the transaction, used for transaction execution.
     */
    String input = "0x";

    /**
     * The amount of KLAY in peb to be transferred.
     */
    String value;


    /**
     * LegacyTransaction Builder class
     */
    public static class Builder extends AbstractTransaction.Builder<LegacyTransaction.Builder> {
        private String to;
        private String value;
        private String input = "0x";

        public Builder(String to) {
            super(TransactionType.TxTypeLegacyTransaction.toString(), TransactionType.TxTypeLegacyTransaction.getType());
            setTo(to);
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setValue(BigInteger value) {
            setValue(Numeric.toHexStringWithPrefix(value));
            return this;
        }

        public Builder setInput(String input) {
            this.input = input;
            return this;
        }

        private Builder setTo(String to) {
            if(!to.equals("0x") && !Utils.isAddress(to)) {
                throw new IllegalArgumentException("Invalid address.");
            }
            this.to = to;
            return this;
        }

        public LegacyTransaction build() {
            return new LegacyTransaction(this);
        }
    }

    /**
     * Creates an LegacyTransaction instance.
     * @param builder LegacyTransaction.Builder instance.
     */
    private LegacyTransaction(Builder builder) {
        super(builder);

        if(builder.to == null || builder.to.isEmpty() || builder.to.equals("0x")) {
            throw new IllegalArgumentException("to is missing");
        }

        if(builder.value == null || builder.value.isEmpty() || builder.value.equals("0x")) {
            throw new IllegalArgumentException("value is missing");
        }

        this.to = builder.to;
        this.value = builder.value;
        this.input = builder.input;
    }

    /**
     * Decodes a RLP-encoded LegacyTransaction string.
     * @param rlpEncoded RLP-encoded LegacyTransaction string
     * @return LegacyTransaction
     */
    public static LegacyTransaction decode(String rlpEncoded) {
        return decode(Numeric.hexStringToByteArray(rlpEncoded));
    }

    /**
     * Decodes a RLP-encoded LegacyTransaction byte array.
     * @param rlpEncoded RLP-encoded LegacyTransaction byte array.
     * @return LegacyTransaction
     */
    public static LegacyTransaction decode(byte[] rlpEncoded) {
        // TxHashRLP = encode([nonce, gasPrice, gas, to, value, input, v, r, s])
        try {
            RlpList rlpList = RlpDecoder.decode(rlpEncoded);
            List<RlpType> values = ((RlpList) rlpList.getValues().get(0)).getValues();

            String nonce = ((RlpString) values.get(0)).asString();
            String gasPrice = ((RlpString) values.get(1)).asString();
            String gas = ((RlpString) values.get(2)).asString();
            String to = ((RlpString) values.get(3)).asString();
            String value = ((RlpString) values.get(4)).asString();
            String input = ((RlpString) values.get(5)).asString();

            LegacyTransaction legacyTransaction = new LegacyTransaction.Builder(to)
                    .setInput(input)
                    .setValue(value)
                    .setNonce(nonce)
                    .setGas(gas)
                    .setGasPrice(gasPrice)
                    .setNonce(nonce)
                    .setTo(to)
                    .build();

            byte[] v = ((RlpString) values.get(6)).getBytes();
            byte[] r = ((RlpString) values.get(7)).getBytes();
            byte[] s = ((RlpString) values.get(8)).getBytes();
            KlaySignatureData signatureData = new KlaySignatureData(v, r, s);

            legacyTransaction.appendSignatures(signatureData);
            return legacyTransaction;
        } catch (Exception e) {
            throw new RuntimeException("There is an error while decoding process.");
        }
    }

    /**
     * Appends signatures array to transaction.
     * Legacy transaction cannot have more than one signature, so an error occurs if the transaction already has a signature.
     * @param signatureData KlaySignatureData instance contains ECDSA signature data
     */
    @Override
    public void appendSignatures(KlaySignatureData signatureData) {
        if(this.getSignatures().size() != 0) {
            throw new RuntimeException("Signatures already defined." + TransactionType.TxTypeLegacyTransaction.toString() + " cannot include more than one signature.");
        }

        super.appendSignatures(signatureData);
    }

    /**
     * Appends signatures array to transaction.
     * Legacy transaction cannot have more than one signature, so an error occurs if the transaction already has a signature.
     * @param signatureData List of KlaySignatureData contains ECDSA signature data
     */
    @Override
    public void appendSignatures(List<KlaySignatureData> signatureData) {
        if(this.getSignatures().size() != 0) {
            throw new RuntimeException("Signatures already defined." + TransactionType.TxTypeLegacyTransaction.toString() + " cannot include more than one signature.");
        }

        if(signatureData.size() != 1) {
            throw new RuntimeException("Signatures are too long " + TransactionType.TxTypeLegacyTransaction.toString() + " cannot include more than one signature.");
        }

        super.appendSignatures(signatureData);
    }

    /**
     * Returns the RLP-encoded string of this transaction (i.e., rawTransaction).
     * @return String
     */
    @Override
    public String getRLPEncoding() {
        this.validateOptionalValues();
        //TxHashRLP = encode([nonce, gasPrice, gas, to, value, input, v, r, s])
        List<RlpType> rlpTypeList = new ArrayList<>();
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getNonce())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getGasPrice())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getGas())));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(this.getTo())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getValue())));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(this.getInput())));
        KlaySignatureData signatureData = this.getSignatures().get(0);
        rlpTypeList.addAll(signatureData.toRlpList().getValues());

        byte[] encoded = RlpEncoder.encode(new RlpList(rlpTypeList));
        String encodedStr = Numeric.toHexString(encoded);

        return encodedStr;
    }


    @Override
    public String getCommonRLPEncodingForSignature() {
        return getRLPEncodingForSignature();
    }

    /**
     * Returns the RLP-encoded string to make the signature of this transaction.
     * @return String
     */
    @Override
    public String getRLPEncodingForSignature() {
        this.validateOptionalValues();

        List<RlpType> rlpTypeList = new ArrayList<>();
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getNonce())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getGasPrice())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getGas())));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(this.getTo())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getValue())));
        rlpTypeList.add(RlpString.create(Numeric.hexStringToByteArray(this.getInput())));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getChainId())));
        rlpTypeList.add(RlpString.create(0));
        rlpTypeList.add(RlpString.create(0));

        byte[] encoded = RlpEncoder.encode(new RlpList(rlpTypeList));
        String encodedStr = Numeric.toHexString(encoded);

        return encodedStr;
    }

    /**
     * Check equals txObj passed parameter and Current instance.
     * @param obj The AbstractTransaction Object to compare
     * @param checkSig Check whether signatures field is equal.
     * @return boolean
     */
    @Override
    public boolean checkTxField(AbstractTransaction obj, boolean checkSig) {
        if(!super.checkTxField(obj, checkSig)) return false;
        if(!(obj instanceof LegacyTransaction)) return false;
        LegacyTransaction txObj = (LegacyTransaction)obj;

        if(!this.getTo().toLowerCase().equals(txObj.getTo().toLowerCase())) return false;
        if(!Numeric.toBigInt(this.getValue()).equals(Numeric.toBigInt(txObj.getValue()))) return false;
        if(!this.getInput().equals(txObj.getInput())) return false;

        return true;
    }

    public String getTo() {
        return to;
    }

    public String getInput() {
        return input;
    }

    public String getValue() {
        return value;
    }
}
