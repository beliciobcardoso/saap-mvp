-- Corrige hashes bcrypt incorretos inseridos pela V11 (senha real: adminPass123 / password123)
UPDATE "usuario" SET password = '$2a$10$QLewVA97Pq/ymBSvsIGyC.Yc3bFIqanQQrGE6MvpRTJ1hnYPN3NyS'
WHERE email = 'admin@saap.com';

UPDATE "usuario" SET password = '$2a$10$sabC9OA2oW6F.n2.AyrU7.jlDN2mzMcX2q7LELZO1wMIQV.riziRq'
WHERE email = 'recep@saap.com';
