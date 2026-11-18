// The page of an invitation link (#295): https://<host>/join/<code>. It shows the code and opens the same
// invitation in the app through the orbit:// link the app registers (ADR 0002). The code has the shape the
// rules and the app require: eight characters of this alphabet.
(function () {
  var ALPHABET = /^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$/;
  var TEXT = {
    en: {
      title: "You are invited to Orbit",
      intro: "Open the invitation in the app, or enter this code under \"Got an invite?\".",
      open: "Open in Orbit",
      copy: "Copy the code",
      copied: "Copied",
      get: "Do not have Orbit yet? Install it, then open this link again or enter the code in the app.",
      invalid: "This link is not an invitation. Ask for a new one."
    },
    es: {
      title: "Te han invitado a Orbit",
      intro: "Abre la invitación en la app, o escribe este código en \"¿Tienes una invitación?\".",
      open: "Abrir en Orbit",
      copy: "Copiar el código",
      copied: "Copiado",
      get: "¿Aún no tienes Orbit? Instálala y vuelve a abrir este enlace, o escribe el código en la app.",
      invalid: "Este enlace no es una invitación. Pide uno nuevo."
    },
    ca: {
      title: "T'han convidat a Orbit",
      intro: "Obre la invitació a l'app, o escriu aquest codi a \"Tens una invitació?\".",
      open: "Obre a Orbit",
      copy: "Copia el codi",
      copied: "Copiat",
      get: "Encara no tens Orbit? Instal·la-la i torna a obrir aquest enllaç, o escriu el codi a l'app.",
      invalid: "Aquest enllaç no és una invitació. Demana'n un de nou."
    },
    it: {
      title: "Sei stato invitato su Orbit",
      intro: "Apri l'invito nell'app, oppure inserisci questo codice in \"Hai un invito?\".",
      open: "Apri in Orbit",
      copy: "Copia il codice",
      copied: "Copiato",
      get: "Non hai ancora Orbit? Installala e riapri questo link, oppure inserisci il codice nell'app.",
      invalid: "Questo link non è un invito. Chiedine uno nuovo."
    }
  };

  var language = (navigator.language || "en").slice(0, 2);
  var text = TEXT[language] || TEXT.en;
  document.documentElement.lang = TEXT[language] ? language : "en";
  document.title = text.title;

  function show(id, value) {
    var element = document.getElementById(id);
    if (value !== undefined) element.textContent = value;
    element.hidden = false;
    return element;
  }

  show("title", text.title);
  var match = /^\/join\/([^/?#]+)\/?$/.exec(location.pathname);
  var code = match ? decodeURIComponent(match[1]).toUpperCase() : "";
  if (!ALPHABET.test(code)) {
    show("invalid", text.invalid);
    return;
  }

  show("invitation");
  document.getElementById("intro").textContent = text.intro;
  show("code", code);
  var open = show("open", text.open);
  open.href = "orbit://join/" + code;
  var copy = show("copy", text.copy);
  copy.addEventListener("click", function () {
    if (navigator.clipboard) {
      // Refused (no permission, not a secure context): the button goes, and the code stays on screen to type.
      navigator.clipboard.writeText(code).then(
        function () { copy.textContent = text.copied; },
        function () { copy.hidden = true; }
      );
    }
  });
  show("get", text.get);
})();
