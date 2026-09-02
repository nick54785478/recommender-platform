$headers = @{"Content-Type" = "application/json"}

Write-Host "Injecting User U001 (Action Movie Fan)..."
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U001", "sessionId":"S1", "itemId":"MOV_001", "behaviorType":"PURCHASE"}'
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U001", "sessionId":"S1", "itemId":"MOV_002", "behaviorType":"PURCHASE"}'

Write-Host "Injecting User U002 (Action Movie Fan, watched one more)..."
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U002", "sessionId":"S2", "itemId":"MOV_001", "behaviorType":"PURCHASE"}'
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U002", "sessionId":"S2", "itemId":"MOV_002", "behaviorType":"PURCHASE"}'
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U002", "sessionId":"S2", "itemId":"MOV_003", "behaviorType":"PURCHASE"}'

Write-Host "Injecting User U003 (Comedy Movie Fan)..."
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U003", "sessionId":"S3", "itemId":"MOV_008", "behaviorType":"PURCHASE"}'
Invoke-RestMethod -Uri http://localhost:9001/api/v1/behaviors/log -Method Post -Headers $headers -Body '{"userId":"U003", "sessionId":"S3", "itemId":"MOV_009", "behaviorType":"PURCHASE"}'

Write-Host "Done! Data injected."
