export const socialButtons = {
  google: {
    color: '#4285F4',
    text: 'white',
  },
  facebook: {
    color: '#4267B2',
    text: 'white',
  },
}

const oauth2ProviderIcons: Record<string, string> = {
  google: 'mdi-google',
  facebook: 'mdi-facebook',
  github: 'mdi-github',
}

export function oauth2ProviderIcon(registrationId: string): string {
  return oauth2ProviderIcons[registrationId.toLowerCase()] ?? 'mdi-login-variant'
}
