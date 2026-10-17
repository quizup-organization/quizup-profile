-- V14: La série de victoires des badges devient globale (tous sujets confondus), alignée sur
-- progression_entry.best_win_streak. On retire les paliers Série acquis sous l'ancienne règle
-- « par sujet » qui dépassent la série réelle du joueur. Les autres badges ne sont pas touchés.
DELETE FROM progression_badge pb
USING progression_entry pe
WHERE pb.user_id = pe.user_id
  AND pb.badge IN (
      'SERIES_FER',
      'SERIES_BRONZE',
      'SERIES_ARGENT',
      'SERIES_OR',
      'SERIES_PLATINE',
      'SERIES_EMERAUDE',
      'SERIES_DIAMANT'
  )
  AND pe.best_win_streak < CASE pb.badge
      WHEN 'SERIES_FER' THEN 2
      WHEN 'SERIES_BRONZE' THEN 4
      WHEN 'SERIES_ARGENT' THEN 10
      WHEN 'SERIES_OR' THEN 25
      WHEN 'SERIES_PLATINE' THEN 50
      WHEN 'SERIES_EMERAUDE' THEN 100
      WHEN 'SERIES_DIAMANT' THEN 200
      ELSE 2147483647
  END;
