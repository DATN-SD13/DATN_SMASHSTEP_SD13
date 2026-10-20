-- Chạy một lần trên DB ứng dụng; chỉ thêm hai bảng. Không sửa/xóa dữ liệu cũ.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.employee_import_batch',N'U') IS NULL
BEGIN
 CREATE TABLE dbo.employee_import_batch (
  id varchar(36) NOT NULL CONSTRAINT PK_employee_import_batch PRIMARY KEY,
  payload_hash varchar(64) NOT NULL,
  created_by bigint NOT NULL,
  created_at datetime2 NOT NULL,
  CONSTRAINT FK_employee_import_batch_actor FOREIGN KEY(created_by) REFERENCES dbo.nhan_vien(id)
 );
 CREATE INDEX IX_employee_import_batch_actor ON dbo.employee_import_batch(created_by,created_at DESC);
END;
IF OBJECT_ID(N'dbo.employee_import_mail',N'U') IS NULL
BEGIN
 CREATE TABLE dbo.employee_import_mail (
  id bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_employee_import_mail PRIMARY KEY,
  batch_id varchar(36) NOT NULL,
  employee_id bigint NOT NULL,
  row_number int NOT NULL,
  state varchar(12) NOT NULL,
  secret_cipher varchar(1024) NULL,
  attempts int NOT NULL CONSTRAINT DF_employee_import_mail_attempts DEFAULT 0,
  last_error nvarchar(300) NULL,
  sent_at datetime2 NULL,
  CONSTRAINT FK_employee_import_mail_batch FOREIGN KEY(batch_id) REFERENCES dbo.employee_import_batch(id),
  CONSTRAINT FK_employee_import_mail_employee FOREIGN KEY(employee_id) REFERENCES dbo.nhan_vien(id),
  CONSTRAINT UQ_employee_import_mail_employee UNIQUE(employee_id),
  CONSTRAINT CK_employee_import_mail_state CHECK(state IN ('PENDING','SENT','FAILED')),
  CONSTRAINT CK_employee_import_mail_row CHECK(row_number BETWEEN 2 AND 501),
  CONSTRAINT CK_employee_import_mail_attempts CHECK(attempts>=0)
 );
 CREATE INDEX IX_employee_import_mail_batch ON dbo.employee_import_mail(batch_id,row_number);
END;
COMMIT TRANSACTION;
