ALTER TABLE gestor ADD COLUMN licenca_valida_ate timestamptz;

-- Quem já tinha conta antes desta migração não pode ficar bloqueado de um
-- momento para o outro só por o teste gratuito de 30 dias, contado a partir
-- do registo, já ter passado há muito. Dá-se-lhes uma licença de 2 anos a
-- partir de agora; o admin ajusta-a à mão para quem quiser cobrar já. Contas
-- criadas a partir de agora ficam de fora deste UPDATE e caem no teste
-- gratuito normal.
UPDATE gestor SET licenca_valida_ate = now() + INTERVAL '2 years';
