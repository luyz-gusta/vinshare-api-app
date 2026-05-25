-- Adiciona display_name em users para permitir que ADMIN e ANALYST tenham nome
-- de exibição sem depender da entidade Customer (que só existe para CLIENT).
-- Backfill copia o full_name do customer associado, quando houver.

ALTER TABLE users ADD COLUMN display_name VARCHAR(120);

UPDATE users u
   SET display_name = c.full_name
  FROM customers c
 WHERE c.user_id = u.id;
