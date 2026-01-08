#!/bin/bash

# ==========================================
# Configurações (podem ser sobrescritas por env)
# ==========================================

APP_USER=${APP_USER:-"TEMPLATEBANKING_ADM"}
APP_PASS=${APP_PASS:-"adm"}

echo "=========================================="
echo "  Oracle App User & Schema Setup"
echo "=========================================="
echo "App user..........: ${APP_USER}"
echo "App password......: ${APP_PASS}"
echo "ORACLE_HOME.......: ${ORACLE_HOME}"
echo "=========================================="


# ==========================================
# Execução SQL como SYSDBA
# ==========================================

"${ORACLE_HOME}"/bin/sqlplus -s "/ as sysdba" <<EOF

SET ECHO ON
SET SERVEROUTPUT ON

DECLARE
  v_pdb_name   VARCHAR2(128);
  v_user_count INTEGER;
BEGIN
  -- 1) Descobre a PDB "de aplicação" (primeira PDB aberta diferente de PDB$SEED)
  SELECT name
  INTO   v_pdb_name
  FROM   (
           SELECT name
           FROM   v\$pdbs
           WHERE  name <> 'PDB\$SEED'
           ORDER  BY con_id
         )
  WHERE  ROWNUM = 1;

  dbms_output.put_line('PDB detectada: ' || v_pdb_name);

  -- 2) Abre a PDB em READ WRITE (se já estiver aberta, não dá erro)
  BEGIN
    EXECUTE IMMEDIATE 'ALTER PLUGGABLE DATABASE ' || v_pdb_name || ' OPEN READ WRITE';
    dbms_output.put_line('PDB ' || v_pdb_name || ' aberta em READ WRITE.');
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -65011 THEN
        dbms_output.put_line('PDB ' || v_pdb_name || ' não existe (ORA-65011).');
        RAISE;
      ELSE
        dbms_output.put_line('Aviso ao abrir PDB: ' || SQLERRM);
      END IF;
  END;

  -- 3) Troca contexto para a PDB
  EXECUTE IMMEDIATE 'ALTER SESSION SET CONTAINER = ' || v_pdb_name;
  dbms_output.put_line('Sessão alterada para PDB: ' || v_pdb_name);

  -- 4) Verifica se o usuário já existe
  SELECT COUNT(*)
  INTO   v_user_count
  FROM   dba_users
  WHERE  username = UPPER('${APP_USER}');

  IF v_user_count = 0 THEN
    dbms_output.put_line('Usuário ${APP_USER} não existe. Criando...');

    EXECUTE IMMEDIATE '
      CREATE USER ${APP_USER} IDENTIFIED BY "${APP_PASS}"
        DEFAULT TABLESPACE USERS
        TEMPORARY TABLESPACE TEMP
        QUOTA UNLIMITED ON USERS
    ';

  ELSE
    dbms_output.put_line('Usuário ${APP_USER} já existe. Pulando criação.');
  END IF;

  -- 5) Concede privilégios (se já tiver, o Oracle ignora)
  dbms_output.put_line('Concedendo privilégios para ${APP_USER}...');

  EXECUTE IMMEDIATE 'GRANT CONNECT, RESOURCE TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE TABLE TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE VIEW TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE SEQUENCE TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE PROCEDURE TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE TRIGGER TO ${APP_USER}';
  EXECUTE IMMEDIATE 'GRANT CREATE SYNONYM TO ${APP_USER}';

  -- 6) Garante que a tabela POSTS exista
  dbms_output.put_line('Garantindo que a tabela POSTS exista em ${APP_USER}...');

  BEGIN
    EXECUTE IMMEDIATE '
      CREATE TABLE ${APP_USER}.POSTS (
        ID       NUMBER(19)      NOT NULL,
        TITLE    VARCHAR2(255)   NOT NULL,
        USER_ID  NUMBER(19)      NOT NULL,
        BODY     CLOB            NOT NULL,
        CONSTRAINT PK_POSTS PRIMARY KEY (ID)
      )
    ';
    dbms_output.put_line('Tabela POSTS criada.');
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -955 THEN
        -- ORA-00955: name is already used by an existing object
        dbms_output.put_line('Tabela POSTS já existe. Pulando criação.');
      ELSE
        dbms_output.put_line('Erro ao criar tabela POSTS: ' || SQLERRM);
        RAISE;
      END IF;
  END;
END;
/

PROMPT >>> FIM do script de criação de usuário e schema.

EXIT;
EOF

echo "=========================================="
echo "User ${APP_USER} configurado (ou já existente) na PDB detectada"
echo "=========================================="