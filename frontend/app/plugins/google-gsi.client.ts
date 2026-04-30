export default defineNuxtPlugin(() => {
  useHead({
    script: [
      {
        src: 'https://accounts.google.com/gsi/client',
        async: true,
        defer: true,
      },
    ],
  })
})
