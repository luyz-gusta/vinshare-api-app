// Endurecimento do App Service existente do VIN Share (IaC versionada e revisável).
@description('Nome do App Service existente')
param appName string = 'vinshare-api'

@description('Runtime atual do App Service (conferir com az webapp config show)')
param linuxFxVersion string = 'JAVA|21-java21'

resource site 'Microsoft.Web/sites@2023-12-01' existing = {
  name: appName
}

// TLS mínimo 1.2, FTP desligado, HTTP/2, always on, health check e logs do servidor.
resource webConfig 'Microsoft.Web/sites/config@2023-12-01' = {
  // checkov:skip=CKV_AZURE_80: aplicação Java 21, não usa .NET Framework
  // checkov:skip=CKV_AZURE_88: API stateless, não monta armazenamento (Azure Files)
  // checkov:skip=CKV_AZURE_13: autenticação feita pela própria API (JWT); Easy Auth bloquearia /auth/login do app
  // checkov:skip=CKV_AZURE_222: API pública consumida pelo app mobile; protegida por TLS, JWT, rate limit e RBAC
  parent: site
  name: 'web'
  properties: {
    linuxFxVersion: linuxFxVersion
    minTlsVersion: '1.2'
    scmMinTlsVersion: '1.2'
    ftpsState: 'Disabled'
    http20Enabled: true
    alwaysOn: true
    healthCheckPath: '/api/v1/actuator/health'
    httpLoggingEnabled: true
    requestTracingEnabled: true
    detailedErrorLoggingEnabled: true
  }
}

// Publicação por FTP com usuário e senha desligada (deploy só pelo pipeline).
resource ftpPolicy 'Microsoft.Web/sites/basicPublishingCredentialsPolicies@2023-12-01' = {
  parent: site
  name: 'ftp'
  properties: {
    allow: false
  }
}
