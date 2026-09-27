-- Concessionária de relacionamento do cliente. Define qual analista pode ver os
-- dados pessoais do cliente (escopo por concessionária, OWASP API1 - BOLA).
ALTER TABLE customers ADD COLUMN home_dealership_id uuid REFERENCES dealerships(id);

-- 1) Clientes com histórico: concessionária do serviço mais recente.
UPDATE customers c
   SET home_dealership_id = last_service.dealership_id
  FROM (
        SELECT DISTINCT ON (v.customer_id) v.customer_id, s.dealership_id
          FROM services s
          JOIN vehicles v ON v.id = s.vehicle_id
         ORDER BY v.customer_id, s.performed_at DESC
       ) last_service
 WHERE last_service.customer_id = c.id;

-- 2) Clientes sem histórico (base sintética): distribuição determinística entre
--    as concessionárias, para que todo lead tenha uma concessionária responsável.
WITH d AS (
    SELECT id,
           (row_number() OVER (ORDER BY name, id)) - 1 AS idx,
           count(*) OVER ()                            AS total
      FROM dealerships
), pending AS (
    SELECT id, (row_number() OVER (ORDER BY created_at, id)) - 1 AS idx
      FROM customers
     WHERE home_dealership_id IS NULL
)
UPDATE customers c
   SET home_dealership_id = d.id
  FROM pending p
  JOIN d ON d.idx = p.idx % d.total
 WHERE c.id = p.id;

CREATE INDEX idx_customers_home_dealership ON customers (home_dealership_id);
