package zio.config.actions

import zio.config.ConfigDescriptor
import zio.config.actions.ConfigDocs.KeyDescription

final case class ConfigDocs(configKeysAndDescription: List[KeyDescription], or: Option[ConfigDocs])

// Man page
object ConfigDocs {
  final case class KeyDescription(path: String, list: List[String])

  final def docs[A](config: ConfigDescriptor[A]): ConfigDocs = {
    def loop[B](
      acc: List[String],
      previousDescription: String,
      config: ConfigDescriptor[B],
      desc: ConfigDocs
    ): ConfigDocs =
      config match {
        case ConfigDescriptor.Empty() => desc
        case ConfigDescriptor.Source(path, _) =>
          ConfigDocs(
            List(KeyDescription(path, if (previousDescription.isEmpty) acc else previousDescription :: acc)),
            None
          )
        case ConfigDescriptor.Describe(c, message) => loop(message :: acc, previousDescription, c, desc)
        case ConfigDescriptor.Optional(c)          => loop(acc, previousDescription, c, desc)
        case ConfigDescriptor.MapEither(c, _, _)   => loop(acc, previousDescription, c, desc)
        case ConfigDescriptor.OnError(c, _)        => loop(acc, previousDescription, c, desc)
        case ConfigDescriptor.Zip(left, right) =>
          ConfigDocs(
            loop(acc, previousDescription, left, desc).configKeysAndDescription ++ loop(
              acc,
              previousDescription,
              right,
              desc
            ).configKeysAndDescription,
            None
          )
        case ConfigDescriptor.Or(left, right) =>
          ConfigDocs(
            loop(acc, previousDescription, left, desc).configKeysAndDescription,
            Some(loop(acc, previousDescription, right, desc))
          )
      }

    loop(Nil, "", config, ConfigDocs(Nil, None))
  }
}
