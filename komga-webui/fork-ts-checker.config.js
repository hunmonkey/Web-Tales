module.exports = {
  typescript: {
    memoryLimit: Number(process.env.VUE_FORK_TS_CHECKER_MEMORY_LIMIT || 8192),
    configOverwrite: {
      include: [
        'src/**/*.ts',
        'src/**/*.tsx',
        'src/**/*.vue',
      ],
      exclude: [
        'node_modules',
        'tests',
      ],
    },
  },
}
