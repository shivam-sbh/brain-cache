package com.braincache.service;

/**
 * Dark/terminal-themed HTML shell shared by every outbound email. Rules are inlined
 * on each element (not a <style> block) because Gmail's web client strips <head>.
 * Table-based layout + mso conditional comments keep Outlook desktop (Word rendering
 * engine) from collapsing the columns; gradients get a solid bgcolor fallback since
 * Outlook doesn't render background-image at all.
 *
 * Public (not package-private) so scheduler jobs in another package can call wrap()
 * directly and hand the result straight to EmailService.
 */
public final class EmailTemplate {

    private EmailTemplate() {}

    public static String wrap(String jobLabel, String dateStamp, String preheader,
                               String heroNumber, String heroTitle, String heroSubtitleHtml,
                               String sectionLabel, String rowsHtml,
                               String ctaLabel, String appUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>")
          .append("<html lang=\"en\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">")
          .append("<head>")
          .append("<meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
          .append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">")
          .append("<meta name=\"color-scheme\" content=\"dark light\">")
          .append("<meta name=\"supported-color-schemes\" content=\"dark light\">")
          .append("<title>").append(escape(jobLabel)).append("</title>")
          .append("<!--[if mso]><style>table{border-collapse:collapse;}</style><![endif]-->")
          .append("</head>")
          .append("<body style=\"margin:0;padding:0;background-color:#08090b;\">")
          .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;\">")
          .append(escape(preheader)).append("</div>")
          .append("<div style=\"display:none;max-height:0;overflow:hidden;mso-hide:all;\">")
          .append("&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;</div>")

          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#08090b\" style=\"background-color:#08090b;\">")
          .append("<tr><td align=\"center\" style=\"padding:28px 12px 40px 12px;\">")
          .append("<!--[if mso]><table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td><![endif]-->")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:600px;width:100%;\">")

          // gradient bar
          .append("<tr><td bgcolor=\"#6d63ff\" height=\"4\" style=\"background-color:#6d63ff;background-image:linear-gradient(90deg,#6d63ff,#4b8bff,#00e0c6);height:4px;line-height:4px;font-size:0;\">&nbsp;</td></tr>")

          // header
          .append("<tr><td bgcolor=\"#101318\" style=\"background-color:#101318;padding:22px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;\">")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
          .append("<td style=\"font-family:'Courier New', Courier, monospace;font-size:18px;font-weight:bold;color:#ffffff;letter-spacing:-0.5px;\">")
          .append("<span style=\"color:#6d63ff;\">&#9646;</span>&nbsp; brain&#8209;cache</td>")
          .append("<td align=\"right\" style=\"font-family:'Courier New', Courier, monospace;font-size:11px;color:#626973;letter-spacing:1px;\">")
          .append(escape(jobLabel)).append("&nbsp;/&nbsp;").append(escape(dateStamp)).append("</td>")
          .append("</tr></table></td></tr>")

          // hero
          .append("<tr><td bgcolor=\"#0b0d10\" style=\"background-color:#0b0d10;padding:38px 28px 30px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;border-top:1px solid #23272f;\">")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
          .append("<td width=\"110\" valign=\"top\" style=\"font-family:'Courier New', Courier, monospace;font-size:64px;line-height:58px;font-weight:bold;color:#6d63ff;\">")
          .append(escape(heroNumber)).append("</td>")
          .append("<td valign=\"top\" style=\"padding-left:6px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">")
          .append("<tr><td style=\"font-family:Helvetica, Arial, sans-serif;font-size:22px;line-height:28px;font-weight:bold;color:#e7e9ec;padding-bottom:8px;\">")
          .append(escape(heroTitle)).append("</td></tr>")
          .append("<tr><td style=\"font-family:Helvetica, Arial, sans-serif;font-size:13px;line-height:20px;color:#99a0aa;\">")
          .append(heroSubtitleHtml).append("</td></tr>")
          .append("</table></td></tr></table></td></tr>")

          // section label + rule
          .append("<tr><td bgcolor=\"#0b0d10\" style=\"background-color:#0b0d10;padding:0 28px 14px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;\">")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">")
          .append("<tr><td style=\"font-family:'Courier New', Courier, monospace;font-size:11px;letter-spacing:2px;color:#00e0c6;padding-bottom:10px;\">// ")
          .append(escape(sectionLabel)).append("</td></tr>")
          .append("<tr><td height=\"1\" bgcolor=\"#23272f\" style=\"background-color:#23272f;height:1px;line-height:1px;font-size:0;\">&nbsp;</td></tr>")
          .append("</table></td></tr>")

          // rows
          .append("<tr><td bgcolor=\"#0b0d10\" style=\"background-color:#0b0d10;padding:0 28px 8px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;\">")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">")
          .append(rowsHtml)
          .append("</table></td></tr>");

        boolean showCta = appUrl != null && (appUrl.startsWith("http://") || appUrl.startsWith("https://"));
        if (showCta) {
            sb.append("<tr><td bgcolor=\"#0b0d10\" align=\"center\" style=\"background-color:#0b0d10;padding:22px 28px 34px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;\">")
              .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
              .append("<td align=\"center\" bgcolor=\"#6d63ff\" style=\"background-color:#6d63ff;background-image:linear-gradient(140deg,#6d63ff,#4b8bff);border-radius:6px;\">")
              .append("<a href=\"").append(escape(appUrl)).append("\" target=\"_blank\" style=\"display:block;padding:15px 40px;font-family:'Courier New', Courier, monospace;font-size:15px;font-weight:bold;letter-spacing:0.5px;color:#ffffff;text-decoration:none;border-radius:6px;\">")
              .append(escape(ctaLabel)).append(" &#9654;</a></td>")
              .append("</tr></table></td></tr>");
        } else {
            sb.append("<tr><td bgcolor=\"#0b0d10\" style=\"background-color:#0b0d10;padding:6px 28px 30px 28px;border-left:1px solid #23272f;border-right:1px solid #23272f;\">&nbsp;</td></tr>");
        }

        // footer
        sb.append("<tr><td bgcolor=\"#101318\" style=\"background-color:#101318;padding:18px 28px;border:1px solid #23272f;\">")
          .append("<span style=\"font-family:'Courier New', Courier, monospace;font-size:11px;color:#626973;\">brain-cache &middot; ")
          .append(escape(jobLabel.toLowerCase())).append("</span>")
          .append("</td></tr>")

          .append("</table>")
          .append("<!--[if mso]></td></tr></table><![endif]-->")
          .append("</td></tr></table>")
          .append("</body></html>");
        return sb.toString();
    }

    public static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
