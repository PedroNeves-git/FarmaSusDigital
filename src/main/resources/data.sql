INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Losartana Potássica 50mg', 150)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Dipirona Sódica 500mg', 200)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Metformina 850mg', 120)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Paracetamol 500mg', 250)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Omeprazol 20mg', 180)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Amoxicilina 500mg', 90)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Sinvastatina 20mg', 100)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Hidroclorotiazida 25mg', 130)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Ácido Acetilsalicílico 100mg', 160)
    ON CONFLICT (nome) DO NOTHING;
INSERT INTO medicamento (nome, quantidade_estoque) VALUES ('Enalapril 20mg', 110)
    ON CONFLICT (nome) DO NOTHING;
