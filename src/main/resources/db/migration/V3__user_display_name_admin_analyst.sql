-- V2 só preencheu display_name a partir de customers.full_name (CLIENT).
-- ADMIN e ANALYST não têm Customer associado e ficaram com display_name NULL,
-- fazendo /me devolver fullName: null. Esta migration cobre os dois casos:
--   - ANALYST: pega o nome real da tabela analysts (analysts.full_name)
--   - ADMIN: fallback humano padrão "Operador Ford"

UPDATE users u
   SET display_name = a.full_name
  FROM analysts a
 WHERE a.user_id = u.id
   AND u.display_name IS NULL;

UPDATE users u
   SET display_name = 'Operador Ford'
 WHERE u.role = 'ADMIN'::public.user_role
   AND u.display_name IS NULL;
