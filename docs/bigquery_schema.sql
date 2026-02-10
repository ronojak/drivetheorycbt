-- BigQuery table schema for daily paid exports
-- Replace `your_dataset.your_table` before executing

CREATE TABLE IF NOT EXISTS `your_dataset.your_table` (
  paymentId STRING,
  userId STRING,
  status STRING,
  planId STRING,
  amount INT64,
  phoneLast4 STRING,
  mpesaReceipt STRING,
  checkoutRequestId STRING,
  merchantRequestId STRING,
  resultCode INT64,
  resultDesc STRING,
  createdAt TIMESTAMP,
  updatedAt TIMESTAMP
);

