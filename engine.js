/**
 * This solution to line numbering was proposed by the Marp author
 * in https://github.com/orgs/marp-team/discussions/164#discussioncomment-6141356
 * 
 * It will only work when used with marp-cli - it's not included by VS Code plugin
 * 
 * This code has been modified not to break on `derives` keyword in scala, where line can start with </span>
 */

module.exports = ({ marp }) =>
  marp.use(({ marpit }) => {
    const { highlighter } = marpit

    marpit.highlighter = function (...args) {
      const original = highlighter.apply(this, args)
      const listItems = original
        .split(/\n(?!$)/)
        .map(
          (line) => {
            if (line.startsWith("</span>"))
              return `</span><li><span data-marp-line-number></span><span>${line.substring("</span>".length)}</span></li>`
            else
              return `<li><span data-marp-line-number></span><span>${line}</span></li>`
          }
        )

      return `<ol>${listItems.join('')}</ol>`
    }
  })