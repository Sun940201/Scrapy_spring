/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Vince Bickers
 */
@Deprecated
public class GraphTransactionStatus implements TransactionStatus {

 private final Logger logger = LoggerFactory.getLogger(GraphTransactionStatus.class);

 private final Transaction transaction;
 private boolean newTransaction = false;

 public GraphTransactionStatus(Session session, TransactionDefinition transactionDefinition) {

  Transaction tx;
  int propagation = transactionDefinition.getPropagationBehavior();

  if (propagation == TransactionDefinition.PROPAGATION_REQUIRES_NEW)  {
   logger.debug("Creating new transaction or extending existing one");
   if (transactionDefinition.isReadOnly()) {
    tx = session.beginTransaction(Transaction.Type.READ_ONLY);
   } else {
    tx = session.getTransaction();
    if (tx == null) {
     tx = session.beginTransaction(Transaction.Type.READ_WRITE);
    } else {
     tx = session.beginTransaction(); // use type of existing transaction
    }
   }
   newTransaction = true;
  } else if (propagation == TransactionDefinition.PROPAGATION_REQUIRED) {
   tx = session.getTransaction();
   if (tx == null
     || tx.status().equals(Transaction.Status.CLOSED)
     || tx.status().equals(Transaction.Status.COMMITTED)
     || tx.status().equals(Transaction.Status.ROLLEDBACK)) {
    if (transactionDefinition.isReadOnly()) {
     tx = session.beginTransaction(Transaction.Type.READ_ONLY);
    } else {
     tx = session.beginTransaction(Transaction.Type.READ_WRITE);
    }
    newTransaction = true;
    logger.debug("Creating new transaction");
   } else {
    logger.debug("Joining existing transaction");
   }
  } else {
   throw new RuntimeException("Transaction propagation type not supported: " + transactionDefinition.getPropagationBehavior());
  }

  this.transaction = tx;
 }

 @Override
 public boolean isNewTransaction() {
  logger.debug("isNewTransaction? " + newTransaction);
  return newTransaction;
 }

 @Override
 public boolean hasSavepoint() {
  logger.info("hasSavePoint? - false");
  return false;
 }

 @Override
 public void setRollbackOnly() {
  logger.info("setRollbackOnly - unsupported");
 }

 @Override
 public boolean isRollbackOnly() {
  logger.info("isRollbackOnly? - false");
  return false;
 }

 @Override
 public void flush() {
  logger.info("flush - unsupported");
 }

 @Override
 public boolean isCompleted() {
  boolean completed = transaction.status().equals(Transaction.Status.ROLLEDBACK) ||
    transaction.status().equals(Transaction.Status.COMMITTED) ||
    transaction.status().equals(Transaction.Status.CLOSED);

  logger.debug("isCompleted? " + completed);

  return completed;


 }

 @Override
 public Object createSavepoint() throws TransactionException {
  logger.info("createSavepoint - unsupported");
  return null;
 }

 @Override
 public void rollbackToSavepoint(Object o) throws TransactionException {
  logger.info("rollbackToSavepoint - unsupported");
 }

 @Override
 public void releaseSavepoint(Object o) throws TransactionException {
  logger.info("releaseSavepoint - unsupported");
 }

 public Transaction getTransaction() {
  return transaction;
 }
}